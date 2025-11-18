package com.saamael.infinitylive

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide // <-- Importante para la imagen
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.saamael.infinitylive.databinding.ActivityInicioBinding
import com.saamael.infinitylive.db.PerfilContract // <-- Importante para la BD
import com.saamael.infinitylive.db.PerfilDbHelper // <-- Importante para la BD
import java.io.File // <-- Importante para leer el archivo

class InicioActivity : BaseActivity(),
    HabitoPositivoAdapter.OnHabitoPositivoListener,
    HabitoNegativoAdapter.OnHabitoNegativoListener {

    private lateinit var binding: ActivityInicioBinding

    // Adaptadores
    private var areaAdapter: AreaAdapter? = null
    private var habitoPositivoAdapter: HabitoPositivoAdapter? = null
    private var habitoNegativoAdapter: HabitoNegativoAdapter? = null
    private var userListener: ListenerRegistration? = null

    // Variables de estado
    private var currentHp: Long = 1000L
    private var currentMonedas: Long = 0L // Variable para guardar monedas
    private val areasUsuario = mutableMapOf<String, Area>()

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
            cargarFotoPerfilDashboard() // <-- CARGAR FOTO AL INICIAR

            // Clic en el nombre para ir al Perfil
            binding.tvNombreUsuario.setOnClickListener {
                val intent = Intent(this, PerfilActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                startActivity(intent)
            }

        } else {
            Toast.makeText(this, "Error: No se pudo encontrar al usuario", Toast.LENGTH_LONG).show()
            mAuth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        // Recargar la foto cada vez que la pantalla se muestra (ej. al volver del Perfil)
        cargarFotoPerfilDashboard()
    }

    // --- Lógica de SQLite para la Foto ---
    private fun cargarFotoPerfilDashboard() {
        if (uid == null) return

        val dbHelper = PerfilDbHelper(this)
        val db = dbHelper.readableDatabase

        // Buscamos la foto específica de ESTE usuario (usando su UID)
        val cursor = db.rawQuery(
            "SELECT * FROM ${PerfilContract.Entry.TABLE_NAME} WHERE ${PerfilContract.Entry.COLUMN_USER_UID} = ?",
            arrayOf(uid)
        )

        if (cursor.moveToFirst()) {
            val pathFoto = cursor.getString(cursor.getColumnIndexOrThrow(PerfilContract.Entry.COLUMN_IMAGE_PATH))
            if (!pathFoto.isNullOrEmpty()) {
                // Carga la imagen con Glide
                Glide.with(this)
                    .load(File(pathFoto))
                    .circleCrop()
                    .into(binding.imgAvatar)
            } else {
                binding.imgAvatar.setImageResource(R.drawable.usericon)
            }
        } else {
            // Si no hay registro, imagen por defecto
            binding.imgAvatar.setImageResource(R.drawable.usericon)
        }
        cursor.close()
        // No cerramos la BD para que App Inspection funcione
    }

    // --- Configuración de Firebase ---

    private fun setupUserProfileListener() {
        userListener = db.collection("users").document(uid!!)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                if (snapshot != null && snapshot.exists()) {
                    val nombre = snapshot.getString("nombre")
                    currentHp = snapshot.getLong("avatar_hp") ?: 1000L
                    currentMonedas = snapshot.getLong("monedas") ?: 0L // Guardamos las monedas

                    binding.tvNombreUsuario.text = nombre
                    binding.tvHp.text = "$currentHp / 1000"
                    binding.tvMonedas.text = currentMonedas.toString()

                    // Actualizamos el nombre en el menú lateral también (via BaseActivity)
                    bindingMenu.tvUserName.text = nombre

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
        habitoPositivoAdapter?.listener = this

        val layoutManagerPos = LinearLayoutManager(this)
        binding.rvHabitosPositivos.layoutManager = layoutManagerPos
        binding.rvHabitosPositivos.adapter = habitoPositivoAdapter
        binding.rvHabitosPositivos.itemAnimator = null
        binding.rvHabitosPositivos.isNestedScrollingEnabled = false // Previene conflictos de scroll

        // --- Negativos ---
        val queryNeg: Query = db.collection("users").document(uid!!).collection("habitos")
            .whereEqualTo("es_positivo", false)
        val optionsNeg = FirestoreRecyclerOptions.Builder<Habito>()
            .setQuery(queryNeg, Habito::class.java).build()

        habitoNegativoAdapter = HabitoNegativoAdapter(optionsNeg)
        habitoNegativoAdapter?.listener = this

        val layoutManagerNeg = LinearLayoutManager(this)
        binding.rvHabitosNegativos.layoutManager = layoutManagerNeg
        binding.rvHabitosNegativos.adapter = habitoNegativoAdapter
        binding.rvHabitosNegativos.itemAnimator = null
        binding.rvHabitosNegativos.isNestedScrollingEnabled = false // Previene conflictos de scroll
    }

    // --- Lógica del Juego ---

    override fun onHabitoCompletado(habito: Habito) {
        if (uid == null) return
        val userDocRef = db.collection("users").document(uid!!)

        userDocRef.update("monedas", FieldValue.increment(habito.monedas_ganadas))
            .addOnFailureListener {
                Toast.makeText(this, "Error al actualizar monedas", Toast.LENGTH_SHORT).show()
            }

        if (habito.area_id != null) {
            val areaDocRef = userDocRef.collection("areas").document(habito.area_id!!)
            areaDocRef.update("xp", FieldValue.increment(habito.xp_ganada))
                .addOnSuccessListener { checkLevelUp(habito.area_id!!) }
        }
    }

    override fun onHabitoCometido(habito: Habito) {
        if (uid == null) return
        val userDocRef = db.collection("users").document(uid!!)

        userDocRef.update("avatar_hp", FieldValue.increment(-habito.hp_perdida))
            .addOnFailureListener {
                Toast.makeText(this, "Error al actualizar HP", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkLevelUp(areaId: String) {
        val area = areasUsuario[areaId] ?: return
        val xpNecesaria = area.nivel * 100

        if (area.xp >= xpNecesaria) {
            val xpSobrante = area.xp - xpNecesaria
            val nuevoNivel = area.nivel + 1

            db.collection("users").document(uid!!).collection("areas").document(areaId)
                .update("nivel", nuevoNivel, "xp", xpSobrante)
                .addOnSuccessListener {
                    AlertDialog.Builder(this)
                        .setTitle("¡Nivel Subido!")
                        .setMessage("¡Felicidades! Tu área \"${area.nombre_area}\" ha subido a Nivel $nuevoNivel.")
                        .setPositiveButton("Genial") { dialog, _ -> dialog.dismiss() }
                        .show()
                }
        }
    }

    private fun checkAvatarStatus() {
        if (currentHp <= 0 && !isActivityRunning(RevivirActivity::class.java)) {
            val intent = Intent(this, RevivirActivity::class.java)
            // Pasamos las monedas para el castigo
            intent.putExtra("MONEDAS_ACTUALES", currentMonedas)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun isActivityRunning(activityClass: Class<*>): Boolean {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val tasks = activityManager.getRunningTasks(Integer.MAX_VALUE)
        for (task in tasks) {
            if (task.baseActivity?.className == activityClass.name) {
                return true
            }
        }
        return false
    }

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