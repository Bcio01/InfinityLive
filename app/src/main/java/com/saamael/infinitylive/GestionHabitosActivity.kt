package com.saamael.infinitylive

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import com.saamael.infinitylive.databinding.ActivityGestionHabitosBinding

// 1. Hereda de BaseActivity
class GestionHabitosActivity : BaseActivity() {

    private lateinit var binding: ActivityGestionHabitosBinding

    // Lista para guardar las áreas del usuario (Nombre, ID)
    private val listaAreas = mutableListOf<Area>()
    private var areaSeleccionadaId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 2. Infla el layout y lo añade a la BaseActivity
        binding = ActivityGestionHabitosBinding.inflate(layoutInflater)
        baseBinding.contentFrame.addView(binding.root)

        // 3. Activa el botón del menú
        setupMenuButton(binding.btnMenu)

        // 4. Carga las áreas en el Spinner
        cargarAreasEnSpinner()

        // 5. Configura el Switch (Positivo/Negativo)
        setupSwitchListener()

        // 6. Configura el botón de Guardar
        binding.btnGuardarHabito.setOnClickListener {
            guardarHabito()
        }
    }

    private fun cargarAreasEnSpinner() {
        if (uid == null) return

        db.collection("users").document(uid!!).collection("areas")
            .get()
            .addOnSuccessListener { snapshot ->
                listaAreas.clear()
                val nombresAreas = mutableListOf<String>()

                for (document in snapshot.documents) {
                    val area = document.toObject(Area::class.java)
                    if (area != null) {
                        area.id = document.id // Guarda el ID del documento
                        listaAreas.add(area)
                        nombresAreas.add(area.nombre_area)
                    }
                }

                // Configura el Spinner con los nombres de las áreas
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, nombresAreas)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerAreas.adapter = adapter
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar áreas", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupSwitchListener() {
        binding.switchPositivo.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Es Positivo
                binding.switchPositivo.text = "Hábito Positivo"
                binding.layoutRecompensas.visibility = View.VISIBLE
                binding.layoutCastigo.visibility = View.GONE
            } else {
                // Es Negativo
                binding.switchPositivo.text = "Hábito Negativo"
                binding.layoutRecompensas.visibility = View.GONE
                binding.layoutCastigo.visibility = View.VISIBLE
            }
        }
    }

    private fun guardarHabito() {
        if (uid == null) return

        // 1. Obtener los datos del formulario
        val descripcion = binding.etHabitoDescripcion.text.toString().trim()
        val esPositivo = binding.switchPositivo.isChecked

        // Obtener el ID del área seleccionada en el Spinner
        val posicionSpinner = binding.spinnerAreas.selectedItemPosition
        if (posicionSpinner < 0 || posicionSpinner >= listaAreas.size) {
            Toast.makeText(this, "Por favor, selecciona un área", Toast.LENGTH_SHORT).show()
            return
        }
        areaSeleccionadaId = listaAreas[posicionSpinner].id

        if (descripcion.isEmpty()) {
            binding.etHabitoDescripcion.error = "La descripción es obligatoria"
            return
        }

        // 2. Crear el objeto Habito
        val habito = Habito(
            descripcion = descripcion,
            es_positivo = esPositivo,
            area_id = areaSeleccionadaId
        )

        // 3. Añadir recompensas o castigos
        try {
            if (esPositivo) {
                habito.xp_ganada = binding.etXpGanada.text.toString().toLongOrNull() ?: 0L
                habito.monedas_ganadas = binding.etMonedasGanadas.text.toString().toLongOrNull() ?: 0L
                habito.hp_perdida = 0L // Asegurarse de que sea 0
            } else {
                habito.hp_perdida = binding.etHpPerdida.text.toString().toLongOrNull() ?: 0L
                habito.xp_ganada = 0L // Asegurarse de que sea 0
                habito.monedas_ganadas = 0L // Asegurarse de que sea 0
            }
        } catch (e: NumberFormatException) {
            Toast.makeText(this, "Error: Revisa los números ingresados", Toast.LENGTH_SHORT).show()
            return
        }

        // 4. Guardar en Firestore
        db.collection("users").document(uid!!).collection("habitos")
            .add(habito) // .add() crea un ID automático
            .addOnSuccessListener {
                Toast.makeText(this, "Hábito guardado con éxito", Toast.LENGTH_SHORT).show()
                finish() // Cierra esta actividad y regresa a la anterior (InicioActivity)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}