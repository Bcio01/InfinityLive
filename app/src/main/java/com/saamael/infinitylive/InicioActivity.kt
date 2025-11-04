package com.saamael.infinitylive

import android.content.Intent // <-- ARREGLA 'Unresolved reference Intent'
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.saamael.infinitylive.databinding.ActivityInicioBinding // Se genera de activity_inicio.xml

// 1. Hereda de BaseActivity
class InicioActivity : BaseActivity() {

    private lateinit var binding: ActivityInicioBinding // Binding para activity_inicio.xml

    // Adaptadores
    private var areaAdapter: AreaAdapter? = null
    private var habitoPositivoAdapter: HabitoPositivoAdapter? = null
    private var habitoNegativoAdapter: HabitoNegativoAdapter? = null

    // Listener para los datos del usuario (HP, Monedas)
    private var userListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState) // Llama al onCreate de BaseActivity

        // --- ESTE BLOQUE CORRIGE LOS ERRORES DE BINDING ---
        // 1. Infla el layout de *esta* actividad (activity_inicio.xml)
        binding = ActivityInicioBinding.inflate(layoutInflater)

        // 2. Añade el layout de esta actividad al 'content_frame' de la BaseActivity
        baseBinding.contentFrame.addView(binding.root)
        // --- FIN DEL BLOQUE DE CORRECCIÓN ---

        // 3. Llama a la función de BaseActivity para que el botón ☰ funcione
        //    Pasándole el botón que está en *este* layout (binding.btnMenu)
        setupMenuButton(binding.btnMenu)

        // 4. Configura el resaltado del menú
        highlightActiveMenuItem()

        // 5. Configura el dashboard
        if (uid != null) {
            setupUserProfileListener()
            setupAreaRecyclerView()
            setupHabitosRecyclerView()
        } else {
            Toast.makeText(this, "Error: No se pudo encontrar al usuario", Toast.LENGTH_LONG).show()
            mAuth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    // --- Configuración del Dashboard ---

    private fun setupUserProfileListener() {
        // Escucha en tiempo real el documento del usuario
        userListener = db.collection("users").document(uid!!)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(this, "Error al cargar perfil: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val nombre = snapshot.getString("nombre")
                    val hp = snapshot.getLong("avatar_hp") ?: 1000L
                    val monedas = snapshot.getLong("monedas") ?: 0L

                    // Actualizar el Dashboard
                    binding.tvNombreUsuario.text = nombre
                    binding.tvHp.text = "$hp / 1000"
                    binding.tvMonedas.text = monedas.toString()

                    // Actualizar el menú lateral también
                    bindingMenu.tvUserName.text = nombre
                }
            }
    }

    private fun setupAreaRecyclerView() {
        val query: Query = db.collection("users").document(uid!!).collection("areas")
            .orderBy("nombre_area")

        val options = FirestoreRecyclerOptions.Builder<Area>()
            .setQuery(query, Area::class.java)
            .build()

        areaAdapter = AreaAdapter(options)

        // Simplemente crea y asigna el LayoutManager
        binding.rvAreas.layoutManager = LinearLayoutManager(this)
        binding.rvAreas.adapter = areaAdapter

        // --- INICIO DE LA CORRECCIÓN ---
        binding.rvAreas.itemAnimator = null // Desactiva las animaciones problemáticas

    }



    private fun setupHabitosRecyclerView() {
        // --- Positivos ---
        val queryPos: Query = db.collection("users").document(uid!!)
            .collection("habitos")
            .whereEqualTo("es_positivo", true)

        val optionsPos = FirestoreRecyclerOptions.Builder<Habito>()
            .setQuery(queryPos, Habito::class.java)
            .build()

        habitoPositivoAdapter = HabitoPositivoAdapter(optionsPos)

        binding.rvHabitosPositivos.layoutManager = LinearLayoutManager(this)
        binding.rvHabitosPositivos.adapter = habitoPositivoAdapter

        // --- INICIO DE LA CORRECCIÓN ---
        binding.rvHabitosPositivos.itemAnimator = null
        // --- FIN DE LA CORRECCIÓN ---

        // --- Negativos ---
        val queryNeg: Query = db.collection("users").document(uid!!)
            .collection("habitos")
            .whereEqualTo("es_positivo", false)

        val optionsNeg = FirestoreRecyclerOptions.Builder<Habito>()
            .setQuery(queryNeg, Habito::class.java)
            .build()

        // ... (código existente de query y options)
        habitoNegativoAdapter = HabitoNegativoAdapter(optionsNeg)

        binding.rvHabitosNegativos.layoutManager = LinearLayoutManager(this)
        binding.rvHabitosNegativos.adapter = habitoNegativoAdapter

        // --- INICIO DE LA CORRECCIÓN ---
        binding.rvHabitosNegativos.itemAnimator = null
        // --- FIN DE LA CORRECCIÓN ---
    }



    private fun highlightActiveMenuItem() {
        // Resalta el botón "Inicio" en el menú lateral
        bindingMenu.tvMenuInicio.setTextColor(ContextCompat.getColor(this, R.color.success))
    }

    // --- Manejo del ciclo de vida de los adaptadores ---

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