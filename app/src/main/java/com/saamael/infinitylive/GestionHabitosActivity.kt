package com.saamael.infinitylive

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.saamael.infinitylive.databinding.ActivityGestionHabitosBinding

class GestionHabitosActivity : BaseActivity() {

    private lateinit var binding: ActivityGestionHabitosBinding

    private val listaAreas = mutableListOf<Area>()
    private var areaSeleccionadaId: String? = null

    // Variables de estado
    private var esPositivo: Boolean = true
    private var dificultadSeleccionada: String = "facil"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityGestionHabitosBinding.inflate(layoutInflater)
        baseBinding.contentFrame.addView(binding.root)

        // 1. Cargar las áreas
        cargarAreasEnSpinner()

        // 2. Configurar listeners
        setupTipoHabito()
        setupDificultad()

        binding.btnGuardarHabito.setOnClickListener {
            guardarHabito()
        }

        // 3. Estado inicial visual
        actualizarVisualTipoHabito()
        actualizarVisualDificultad()

        // 4. Mostrar los rangos iniciales en los botones
        actualizarTextoBotonesDificultad()
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
                        area.id = document.id
                        listaAreas.add(area)
                        nombresAreas.add(area.nombre_area)
                    }
                }

                // Usamos android.R.layout solo para el spinner simple, esto está bien
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, nombresAreas)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerAreas.adapter = adapter
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar áreas", Toast.LENGTH_SHORT).show()
            }
    }

    // --- LÓGICA DE UI: TIPO DE HÁBITO ---
    private fun setupTipoHabito() {
        binding.btnTogglePositivo.setOnClickListener {
            esPositivo = true
            actualizarVisualTipoHabito()
            actualizarTextoBotonesDificultad()
        }
        binding.btnToggleNegativo.setOnClickListener {
            esPositivo = false
            actualizarVisualTipoHabito()
            actualizarTextoBotonesDificultad()
        }
    }

    private fun actualizarVisualTipoHabito() {
        val colorActivo = ContextCompat.getColor(this, R.color.divider)
        val colorInactivo = Color.TRANSPARENT

        if (esPositivo) {
            binding.btnTogglePositivo.backgroundTintList = ColorStateList.valueOf(colorActivo)
            binding.btnToggleNegativo.backgroundTintList = ColorStateList.valueOf(colorInactivo)
        } else {
            binding.btnTogglePositivo.backgroundTintList = ColorStateList.valueOf(colorInactivo)
            binding.btnToggleNegativo.backgroundTintList = ColorStateList.valueOf(colorActivo)
        }
    }

    // --- ACTUALIZAR TEXTO CON RANGOS ---
    private fun actualizarTextoBotonesDificultad() {
        if (esPositivo) {
            binding.btnArea1.text = "Fácil\n(5-10 $)"
            binding.btnArea2.text = "Medio\n(10-15 $)"
            binding.btnArea3.text = "Difícil\n(15-20 $)"
        } else {
            binding.btnArea1.text = "Fácil\n(-15 HP)"
            binding.btnArea2.text = "Medio\n(-35 HP)"
            binding.btnArea3.text = "Difícil\n(-65 HP)"
        }
    }

    // --- LÓGICA DE UI: DIFICULTAD ---
    private fun setupDificultad() {
        binding.btnArea1.setOnClickListener {
            dificultadSeleccionada = "facil"
            actualizarVisualDificultad()
        }
        binding.btnArea2.setOnClickListener {
            dificultadSeleccionada = "medio"
            actualizarVisualDificultad()
        }
        binding.btnArea3.setOnClickListener {
            dificultadSeleccionada = "dificil"
            actualizarVisualDificultad()
        }
    }

    private fun actualizarVisualDificultad() {
        val colorFondoActivo = ContextCompat.getColor(this, R.color.divider)
        val colorFondoInactivo = ContextCompat.getColor(this, R.color.surface)

        // Resetear visualmente
        binding.btnArea1.isChecked = false
        binding.btnArea1.backgroundTintList = ColorStateList.valueOf(colorFondoInactivo)

        binding.btnArea2.isChecked = false
        binding.btnArea2.backgroundTintList = ColorStateList.valueOf(colorFondoInactivo)

        binding.btnArea3.isChecked = false
        binding.btnArea3.backgroundTintList = ColorStateList.valueOf(colorFondoInactivo)

        // Activar seleccionado
        when (dificultadSeleccionada) {
            "facil" -> {
                binding.btnArea1.isChecked = true
                binding.btnArea1.backgroundTintList = ColorStateList.valueOf(colorFondoActivo)
            }
            "medio" -> {
                binding.btnArea2.isChecked = true
                binding.btnArea2.backgroundTintList = ColorStateList.valueOf(colorFondoActivo)
            }
            "dificil" -> {
                binding.btnArea3.isChecked = true
                binding.btnArea3.backgroundTintList = ColorStateList.valueOf(colorFondoActivo)
            }
        }
    }

    // --- GUARDADO ---
    private fun guardarHabito() {
        if (uid == null) return

        val descripcion = binding.etHabitoDescripcion.text.toString().trim()

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

        val stats = calcularStatsBase(dificultadSeleccionada, esPositivo)

        val habito = Habito(
            descripcion = descripcion,
            es_positivo = esPositivo,
            area_id = areaSeleccionadaId,
            dificultad = dificultadSeleccionada,
            xp_ganada = stats.xp,
            monedas_ganadas = stats.monedas,
            hp_perdida = stats.hp
        )

        db.collection("users").document(uid!!).collection("habitos")
            .add(habito)
            .addOnSuccessListener {
                Toast.makeText(this, "Hábito creado con éxito", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    data class StatsHabito(val xp: Long, val monedas: Long, val hp: Long)

    private fun calcularStatsBase(dificultad: String, esPositivo: Boolean): StatsHabito {
        var xp = 0L
        var monedas = 0L
        var hp = 0L

        when (dificultad) {
            "facil" -> {
                if (esPositivo) { xp = 10; monedas = 5 } else { hp = 20 }
            }
            "medio" -> {
                if (esPositivo) { xp = 20; monedas = 10 } else { hp = 35 }
            }
            "dificil" -> {
                if (esPositivo) { xp = 35; monedas = 15 } else { hp = 65 }
            }
        }
        return StatsHabito(xp, monedas, hp)
    }
}