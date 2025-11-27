package com.saamael.infinitylive

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.saamael.infinitylive.databinding.ActivityInicioBinding
import com.saamael.infinitylive.db.PerfilContract
import com.saamael.infinitylive.db.PerfilDbHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File

class InicioActivity : BaseActivity(),
    HabitoPositivoAdapter.OnHabitoPositivoListener,
    HabitoNegativoAdapter.OnHabitoNegativoListener {

    private lateinit var binding: ActivityInicioBinding

    // Adaptadores
    private var areaAdapter: AreaAdapter? = null
    private var habitoPositivoAdapter: HabitoPositivoAdapter? = null
    private var habitoNegativoAdapter: HabitoNegativoAdapter? = null

    // Listeners de Firebase (Deben limpiarse en onStop)
    private var userListener: ListenerRegistration? = null
    private var areasListener: ListenerRegistration? = null // NUEVO: Para evitar el crash del mapa

    // Variables de estado
    private var currentHp: Long = 1000L
    private var currentMonedas: Long = 0L
    private val areasUsuario = mutableMapOf<String, Area>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityInicioBinding.inflate(layoutInflater)
        setContentView(binding.root) // Ignoramos layout de BaseActivity

        // Configuración de UI
        setupBottomMenu()
        setupHeaderActions()
        setupHabitoToggles()

        if (uid != null) {
            // Configuramos los Recyclers (pero NO iniciamos la escucha de datos aún)
            setupAreaRecyclerView()
            setupHabitosRecyclerView()
            cargarFotoPerfilDashboard()
        } else {
            Toast.makeText(this, "Error: Usuario no encontrado", Toast.LENGTH_LONG).show()
            mAuth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    fun sincronizarDatosConNube() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser

        // 1. Preparamos el resumen de áreas
        val resumenAreas = mutableMapOf<String, Int>()
        areasUsuario.values.forEach { area ->
            resumenAreas[area.nombre_area] = area.nivel.toInt()
        }

        // Calcular nivel global (opcional)
        val nivelTotal = if (resumenAreas.isNotEmpty()) resumenAreas.values.sum() / resumenAreas.size else 1

        val misDatos = hashMapOf(
            "id" to userId,
            "nombre" to (currentUser?.displayName ?: "Usuario"),
            "vidaActual" to currentHp.toInt(),
            "correo" to (currentUser?.email ?: ""),
            "nivelGlobal" to nivelTotal,
            "areasResumen" to resumenAreas
        )

        db.collection("usuarios").document(userId)
            .set(misDatos, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                android.util.Log.d("Nube", "Perfil sincronizado")
            }
    }

    // --- CICLO DE VIDA ---
    override fun onStart() {
        super.onStart()
        // 1. Iniciamos los adaptadores (Listas visuales)
        areaAdapter?.startListening()
        habitoPositivoAdapter?.startListening()
        habitoNegativoAdapter?.startListening()

        // 2. Iniciamos los listeners de datos (Lógica interna)
        startListeningUser()
        startListeningAreasData() // NUEVO: Iniciamos escucha segura
    }

    override fun onStop() {
        super.onStop()
        // 1. Detenemos adaptadores
        areaAdapter?.stopListening()
        habitoPositivoAdapter?.stopListening()
        habitoNegativoAdapter?.stopListening()

        // 2. MATAMOS LOS LISTENERS (Esto evita el crash)
        userListener?.remove()
        areasListener?.remove() // NUEVO: Limpiamos la memoria
    }

    override fun onResume() {
        super.onResume()
        cargarFotoPerfilDashboard()
        sincronizarDatosConNube()
    }

    // --- Listeners de Datos Seguros ---

    private fun startListeningUser() {
        val safeUid = uid ?: return
        userListener?.remove() // Limpiar previo si existe

        userListener = db.collection("users").document(safeUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                if (snapshot != null && snapshot.exists()) {
                    val nombre = snapshot.getString("nombre")
                    currentHp = snapshot.getLong("avatar_hp") ?: 1000L
                    currentMonedas = snapshot.getLong("monedas") ?: 0L

                    binding.tvNombreUsuario.text = nombre
                    binding.tvHp.text = "$currentHp / 1000"
                    binding.tvMonedas.text = currentMonedas.toString()

                    checkAvatarStatus()
                }
            }
    }

    // NUEVO: Función separada para escuchar datos de áreas sin memory leaks
    private fun startListeningAreasData() {
        val safeUid = uid ?: return
        areasListener?.remove()

        areasListener = db.collection("users").document(safeUid).collection("areas")
            .addSnapshotListener { snapshots, error ->
                if (error != null) return@addSnapshotListener

                // Actualizamos el mapa en memoria
                areasUsuario.clear()
                snapshots?.forEach { doc ->
                    val area = doc.toObject(Area::class.java)
                    area.id = doc.id
                    areasUsuario[area.id] = area
                }
            }
    }

    // --- Configuración Recyclers ---

    private fun setupAreaRecyclerView() {
        // Solo configuramos el adaptador visual, la lógica de datos va en startListeningAreasData
        val query = db.collection("users").document(uid!!).collection("areas").orderBy("nombre_area")
        val options = FirestoreRecyclerOptions.Builder<Area>().setQuery(query, Area::class.java).build()

        areaAdapter = AreaAdapter(options)
        binding.rvAreas.layoutManager = GridLayoutManager(this, 2)
        binding.rvAreas.adapter = areaAdapter
        binding.rvAreas.itemAnimator = null
    }

    private fun setupHabitosRecyclerView() {
        // --- Positivos ---
        val queryPos = db.collection("users").document(uid!!).collection("habitos").whereEqualTo("es_positivo", true)
        val optionsPos = FirestoreRecyclerOptions.Builder<Habito>().setQuery(queryPos, Habito::class.java).build()
        habitoPositivoAdapter = HabitoPositivoAdapter(optionsPos)
        habitoPositivoAdapter?.listener = this

        // USAMOS EL NUEVO MANAGER SEGURO
        binding.rvHabitosPositivos.layoutManager = WrapContentLinearLayoutManager(this)
        binding.rvHabitosPositivos.adapter = habitoPositivoAdapter
        binding.rvHabitosPositivos.itemAnimator = null
        binding.rvHabitosPositivos.isNestedScrollingEnabled = false

        // --- Negativos ---
        val queryNeg = db.collection("users").document(uid!!).collection("habitos").whereEqualTo("es_positivo", false)
        val optionsNeg = FirestoreRecyclerOptions.Builder<Habito>().setQuery(queryNeg, Habito::class.java).build()
        habitoNegativoAdapter = HabitoNegativoAdapter(optionsNeg)
        habitoNegativoAdapter?.listener = this

        // USAMOS EL NUEVO MANAGER SEGURO
        binding.rvHabitosNegativos.layoutManager = WrapContentLinearLayoutManager(this)
        binding.rvHabitosNegativos.adapter = habitoNegativoAdapter
        binding.rvHabitosNegativos.itemAnimator = null
        binding.rvHabitosNegativos.isNestedScrollingEnabled = false
    }
    // --- Helpers Visuales ---

    private fun cargarFotoPerfilDashboard() {
        if (uid == null) return

        // CORRECCIÓN GLIDE: Verificar si la actividad está viva
        if (isDestroyed || isFinishing) return

        val dbHelper = PerfilDbHelper(this)
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM ${PerfilContract.Entry.TABLE_NAME} WHERE ${PerfilContract.Entry.COLUMN_USER_UID} = ?",
            arrayOf(uid)
        )
        if (cursor.moveToFirst()) {
            val pathFoto = cursor.getString(cursor.getColumnIndexOrThrow(PerfilContract.Entry.COLUMN_IMAGE_PATH))
            if (!pathFoto.isNullOrEmpty()) {
                Glide.with(this).load(File(pathFoto)).circleCrop().into(binding.imgAvatar)
            } else {
                binding.imgAvatar.setImageResource(R.drawable.usericon)
            }
        } else {
            binding.imgAvatar.setImageResource(R.drawable.usericon)
        }
        cursor.close()
    }

    private fun setupHeaderActions() {
        val irPerfil = {
            val intent = Intent(this, PerfilActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(intent)
        }
        binding.imgAvatar.setOnClickListener { irPerfil() }
        binding.tvNombreUsuario.setOnClickListener { irPerfil() }

        /*
        binding.btnLogoutHeader.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Cerrar Sesión")
                .setMessage("¿Estás seguro?")
                .setPositiveButton("Sí") { _, _ ->
                    mAuth.signOut()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

         */
    }

    private fun setupBottomMenu() {
        binding.MenuInferior.menuHabitos.setOnClickListener {
            binding.scrollViewContent.smoothScrollTo(0, 0)
        }
        // 4. Botón Rutinas (NUEVO LINK)
        binding.MenuInferior.menuDiarias.setOnClickListener {
            val intent = Intent(this, RutinasActivity::class.java)
            // FLAG_ACTIVITY_REORDER_TO_FRONT es útil si quieres mantener el estado
            // de RutinasActivity si el usuario va y vuelve. Si no, quítalo.
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(intent)
        }
        binding.MenuInferior.fabDiamondContainer.setOnClickListener {
            val intent = Intent(this, GestionHabitosActivity::class.java)
            startActivity(intent)
        }
        binding.MenuInferior.menuSocial.setOnClickListener {
            val intent = Intent(this, SocialActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(intent)
        }
        binding.MenuInferior.menuRecompensas.setOnClickListener {
            val intent = Intent(this, TiendaActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(intent)
        }
    }

    private fun setupHabitoToggles() {
        val btnPositivo = binding.btnTogglePositivo
        val btnNegativo = binding.btnToggleNegativo
        val rvPositivos = binding.rvHabitosPositivos
        val rvNegativos = binding.rvHabitosNegativos

        val colorSuccess = ContextCompat.getColor(this, R.color.success)
        val colorWarning = ContextCompat.getColor(this, R.color.warning)
        val colorInactiveBorder = ContextCompat.getColor(this, R.color.divider)
        val colorInactiveText = ContextCompat.getColor(this, R.color.text_secondary)
        val strokeActive = 4
        val strokeInactive = 2

        // Config inicial
        btnPositivo.strokeColor = ColorStateList.valueOf(colorSuccess)
        btnPositivo.strokeWidth = strokeActive
        btnPositivo.setTextColor(colorSuccess)
        btnNegativo.strokeColor = ColorStateList.valueOf(colorInactiveBorder)
        btnNegativo.strokeWidth = strokeInactive
        btnNegativo.setTextColor(colorInactiveText)

        btnPositivo.setOnClickListener {
            rvPositivos.visibility =android.view.View.VISIBLE
            rvNegativos.visibility = android.view.View.GONE

            btnPositivo.strokeColor = ColorStateList.valueOf(colorSuccess)
            btnPositivo.strokeWidth = strokeActive
            btnPositivo.setTextColor(colorSuccess)

            btnNegativo.strokeColor = ColorStateList.valueOf(colorInactiveBorder)
            btnNegativo.strokeWidth = strokeInactive
            btnNegativo.setTextColor(colorInactiveText)
        }

        btnNegativo.setOnClickListener {
            rvPositivos.visibility = android.view.View.GONE
            rvNegativos.visibility = android.view.View.VISIBLE

            btnNegativo.strokeColor = ColorStateList.valueOf(colorWarning)
            btnNegativo.strokeWidth = strokeActive
            btnNegativo.setTextColor(colorWarning)

            btnPositivo.strokeColor = ColorStateList.valueOf(colorInactiveBorder)
            btnPositivo.strokeWidth = strokeInactive
            btnPositivo.setTextColor(colorInactiveText)
        }
    }

    // --- Lógica Juego ---

    override fun onHabitoCompletado(habito: Habito) {
        val safeUid = uid ?: return
        val userDocRef = db.collection("users").document(safeUid)
        userDocRef.update("monedas", FieldValue.increment(habito.monedas_ganadas))
        if (habito.area_id != null) {
            val areaDocRef = userDocRef.collection("areas").document(habito.area_id!!)
            areaDocRef.update("xp", FieldValue.increment(habito.xp_ganada))
                .addOnSuccessListener { checkLevelUp(habito.area_id!!) }
        }
    }

    override fun onHabitoCometido(habito: Habito) {
        val safeUid = uid ?: return
        val userDocRef = db.collection("users").document(safeUid)
        userDocRef.update("avatar_hp", FieldValue.increment(-habito.hp_perdida))
    }

    override fun onDestroy() {
        super.onDestroy()
        // Detener la escucha de datos de la UI
        areaAdapter?.stopListening()
        habitoPositivoAdapter?.stopListening()
        habitoNegativoAdapter?.stopListening()

        // Nulificar para evitar referencias fantasmas
        binding.rvAreas.adapter = null
        binding.rvHabitosPositivos.adapter = null
        binding.rvHabitosNegativos.adapter = null
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
        if (currentHp <= 0 && !RevivirActivity.isRunning) {
            val intent = Intent(this, RevivirActivity::class.java)
            intent.putExtra("MONEDAS_ACTUALES", currentMonedas)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}

// Clase auxiliar para evitar crashes de RecyclerView
class WrapContentLinearLayoutManager(context: Context) : LinearLayoutManager(context) {
    override fun onLayoutChildren(recycler: androidx.recyclerview.widget.RecyclerView.Recycler?, state: androidx.recyclerview.widget.RecyclerView.State?) {
        try {
            super.onLayoutChildren(recycler, state)
        } catch (e: IndexOutOfBoundsException) {
            android.util.Log.e("TAG", "Inconsistency detected in RecyclerView")
        }
    }
}