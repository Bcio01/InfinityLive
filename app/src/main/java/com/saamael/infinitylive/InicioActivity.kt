package com.saamael.infinitylive

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog // Importa AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.saamael.infinitylive.databinding.ActivityInicioBinding
import android.app.ActivityManager
import android.content.Context


// 2. IMPLEMENTA LAS INTERFACES DE LOS ADAPTADORES
class InicioActivity : BaseActivity(),
    HabitoPositivoAdapter.OnHabitoPositivoListener,
    HabitoNegativoAdapter.OnHabitoNegativoListener {

    private lateinit var binding: ActivityInicioBinding

    // ... (variables de adaptadores y listener)
    private var areaAdapter: AreaAdapter? = null
    private var habitoPositivoAdapter: HabitoPositivoAdapter? = null
    private var habitoNegativoAdapter: HabitoNegativoAdapter? = null
    private var userListener: ListenerRegistration? = null

    // Variable para guardar los datos del usuario (HP y Áreas)
    private var currentHp: Long = 1000L
    private var currentMonedas: Long = 0L
    private val areasUsuario = mutableMapOf<String, Area>() // <AreaID, Objeto Area>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityInicioBinding.inflate(layoutInflater)
        baseBinding.contentFrame.addView(binding.root)

        setupMenuButton(binding.btnMenu)
        highlightActiveMenuItem()

        if (uid != null) {
            setupUserProfileListener()
            setupAreaRecyclerView()
            setupHabitosRecyclerView()

        } else {

        }
    }

    // --- Configuración del Dashboard ---

    private fun setupUserProfileListener() {
        userListener = db.collection("users").document(uid!!)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { /* ... (manejo de error) ... */ return@addSnapshotListener }

                if (snapshot != null && snapshot.exists()) {
                    val nombre = snapshot.getString("nombre")
                    currentHp = snapshot.getLong("avatar_hp") ?: 1000L // Actualiza el HP
                    currentMonedas = snapshot.getLong("monedas") ?: 0L

                    binding.tvNombreUsuario.text = nombre
                    binding.tvHp.text = "$currentHp / 1000"
                    binding.tvMonedas.text = currentMonedas.toString()
                    bindingMenu.tvUserName.text = nombre

                    // Comprobar si el usuario está muerto al cargar
                    checkAvatarStatus()
                }
            }
    }

    private fun setupAreaRecyclerView() {
        val query: Query = db.collection("users").document(uid!!).collection("areas")
            .orderBy("nombre_area")

        val options = FirestoreRecyclerOptions.Builder<Area>()
            .setQuery(query, Area::class.java)
            .build()

        // Guardar las áreas para la lógica de XP
        query.addSnapshotListener { snapshots, _ ->
            areasUsuario.clear()
            snapshots?.forEach { document ->
                val area = document.toObject(Area::class.java)
                area.id = document.id
                areasUsuario[area.id] = area
            }
        }

        areaAdapter = AreaAdapter(options)
        val areaLayoutManager = LinearLayoutManager(this)
        binding.rvAreas.itemAnimator = null // Desactiva las animaciones problemáticas
        binding.rvAreas.layoutManager = areaLayoutManager
        binding.rvAreas.adapter = areaAdapter
        binding.rvAreas.itemAnimator = null
    }

    private fun setupHabitosRecyclerView() {
        // --- Positivos ---
        val queryPos: Query = db.collection("users").document(uid!!).collection("habitos")
            .whereEqualTo("es_positivo", true)
        val optionsPos = FirestoreRecyclerOptions.Builder<Habito>()
            .setQuery(queryPos, Habito::class.java).build()

        habitoPositivoAdapter = HabitoPositivoAdapter(optionsPos)
        // --- 3. ASIGNA EL LISTENER ---
        habitoPositivoAdapter?.listener = this // "this" es InicioActivity
        // ---
        val layoutManagerPos = LinearLayoutManager(this)
        binding.rvAreas.itemAnimator = null // Desactiva las animaciones problemáticas
        binding.rvHabitosPositivos.layoutManager = layoutManagerPos
        binding.rvHabitosPositivos.adapter = habitoPositivoAdapter
        binding.rvHabitosPositivos.itemAnimator = null

        // --- Negativos ---
        val queryNeg: Query = db.collection("users").document(uid!!)
            .collection("habitos")
            .whereEqualTo("es_positivo", false)
        val optionsNeg = FirestoreRecyclerOptions.Builder<Habito>()
            .setQuery(queryNeg, Habito::class.java).build()

        habitoNegativoAdapter = HabitoNegativoAdapter(optionsNeg)
        // --- 3. ASIGNA EL LISTENER ---
        habitoNegativoAdapter?.listener = this // "this" es InicioActivity
        // ---
        val layoutManagerNeg = LinearLayoutManager(this)
        binding.rvAreas.itemAnimator = null // Desactiva las animaciones problemáticas
        binding.rvHabitosNegativos.layoutManager = layoutManagerNeg
        binding.rvHabitosNegativos.adapter = habitoNegativoAdapter
        binding.rvHabitosNegativos.itemAnimator = null
        
    }

    // --- 4. IMPLEMENTA LA LÓGICA DEL JUEGO ---

    /**
     * Se llama cuando el usuario presiona el botón '+' en un hábito positivo.
     */
    override fun onHabitoCompletado(habito: Habito) {
        if (uid == null) return

        val userDocRef = db.collection("users").document(uid!!)

        // Usamos FieldValue.increment() para sumar de forma segura
        userDocRef.update(
            "monedas", FieldValue.increment(habito.monedas_ganadas)
        ).addOnFailureListener {
            Toast.makeText(this, "Error al sumar monedas: ${it.message}", Toast.LENGTH_SHORT).show()
        }

        // Ahora sumamos la XP al área correspondiente
        if (habito.area_id != null) {
            val areaDocRef = userDocRef.collection("areas").document(habito.area_id!!)
            areaDocRef.update("xp", FieldValue.increment(habito.xp_ganada))
                .addOnSuccessListener {
                    // Después de sumar XP, revisamos si el área subió de nivel
                    checkLevelUp(habito.area_id!!)
                }
        }
    }

    /**
     * Se llama cuando el usuario presiona el botón '-' en un hábito negativo.
     */
    override fun onHabitoCometido(habito: Habito) {
        if (uid == null) return

        // TODO: En la rama "funcionalidad/tienda", revisaremos si el usuario
        // tiene un "pase" para este hábito antes de restar HP.

        val userDocRef = db.collection("users").document(uid!!)

        // Usamos FieldValue.increment() con un número negativo para restar HP
        userDocRef.update(
            "avatar_hp", FieldValue.increment(-habito.hp_perdida)
        ).addOnSuccessListener {
            // La variable 'currentHp' se actualizará sola gracias al SnapshotListener,
            // que llamará a checkAvatarStatus().
        }.addOnFailureListener {
            Toast.makeText(this, "Error al restar HP: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Revisa si un área tiene suficiente XP para subir de nivel.
     */
    private fun checkLevelUp(areaId: String) {
        val area = areasUsuario[areaId] ?: return // Obtiene el área del mapa
        val xpNecesaria = area.nivel * 100

        if (area.xp >= xpNecesaria) {
            // ¡SUBIÓ DE NIVEL!
            val xpSobrante = area.xp - xpNecesaria
            val nuevoNivel = area.nivel + 1

            // Actualizamos el área en Firestore
            db.collection("users").document(uid!!).collection("areas").document(areaId)
                .update(
                    "nivel", nuevoNivel,
                    "xp", xpSobrante
                )
                .addOnSuccessListener {
                    // Muestra una alerta de felicitación
                    AlertDialog.Builder(this)
                        .setTitle("¡Nivel Subido!")
                        .setMessage("¡Felicidades! Tu área \"${area.nombre_area}\" ha subido a Nivel $nuevoNivel.")
                        .setPositiveButton("Genial") { dialog, _ -> dialog.dismiss() }
                        .show()
                }
        }
    }

    /**
     * Revisa si el avatar "murió" (HP <= 0).
     */
    private fun checkAvatarStatus() {
        // Esta comprobación evita que la RevivirActivity se lance múltiples veces
        // si el HP sigue en 0 mientras la actividad ya está abierta.
        if (currentHp <= 0 && !isActivityRunning(RevivirActivity::class.java)) {

            // ¡EL AVATAR MURIÓ!
            // Lanza la pantalla de bloqueo (RevivirActivity)
            val intent = Intent(this, RevivirActivity::class.java)
            // Pasamos el total de monedas a la pantalla de muerte
            intent.putExtra("MONEDAS_ACTUALES", currentMonedas)

            // Estos flags evitan que el usuario pueda volver a InicioActivity
            // y borran el historial de navegación.
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)

            // Cierra la InicioActivity actual para que no se quede "debajo"
            finish()
        }
    }

    /**
     * Función de utilidad para comprobar si una actividad ya está en la pantalla.
     */
    private fun isActivityRunning(activityClass: Class<*>): Boolean {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        // Nota: getRunningTasks está obsoleto, pero es la forma más simple
        // para este caso de uso específico.
        val tasks = activityManager.getRunningTasks(Integer.MAX_VALUE)
        for (task in tasks) {
            if (task.baseActivity?.className == activityClass.name) {
                return true
            }
        }
        return false
    }

    // ... (highlightActiveMenuItem, onStart, onStop) ...

    private fun highlightActiveMenuItem() {
        bindingMenu.tvMenuInicio.setTextColor(ContextCompat.getColor(this, R.color.success))
    }

    override fun onStart() {
        super.onStart()
        areaAdapter?.startListening()
        habitoPositivoAdapter?.startListening()
        habitoNegativoAdapter?.startListening()
    }

    override fun onStop() {
        super.onStop()
        areaAdapter?.stopListening()
        habitoPositivoAdapter?.stopListening()
        habitoNegativoAdapter?.stopListening()
        userListener?.remove()
    }
}