package com.saamael.infinitylive

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.saamael.infinitylive.databinding.ActivitySelectAreaBinding

class SelectAreaActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySelectAreaBinding
    private lateinit var mAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private val MIN_AREAS = 3
    private val MAX_AREAS = 12

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelectAreaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Asignar clics a los botones
        binding.btnConfirmarAreas.setOnClickListener {
            it.isEnabled = false // Desactivar para evitar doble clic
            crearUsuarioYAreas()
        }

        binding.btnAnadirArea.setOnClickListener {
            anadirAreaEditText()
        }

        binding.btnQuitarArea.setOnClickListener {
            quitarAreaEditText()
        }
    }

    // --- LÓGICA DINÁMICA DE CAMPOS ---

    private fun anadirAreaEditText() {
        val numeroActualDeAreas = binding.layoutAreasDinamicas.childCount
        if (numeroActualDeAreas < MAX_AREAS) {
            // 1. Crear un nuevo EditText
            val nuevoEditText = EditText(this)

            // 2. Asignarle el estilo (copiado de tu XML)
            val layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            val topMargin = (16 * resources.displayMetrics.density).toInt() // 16dp
            layoutParams.setMargins(0, topMargin, 0, 0)
            nuevoEditText.layoutParams = layoutParams

            nuevoEditText.hint = "Área ${numeroActualDeAreas + 1}"
            nuevoEditText.setBackgroundResource(R.drawable.rounded_input) // Asume que tienes este drawable
            nuevoEditText.inputType = android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            nuevoEditText.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
            nuevoEditText.setHintTextColor(ContextCompat.getColor(this, R.color.text_secondary))

            // 3. Añadirlo al contenedor
            binding.layoutAreasDinamicas.addView(nuevoEditText)

        } else {
            Toast.makeText(this, "Máximo de 12 áreas alcanzado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun quitarAreaEditText() {
        val numeroActualDeAreas = binding.layoutAreasDinamicas.childCount
        if (numeroActualDeAreas > MIN_AREAS) {
            // Borra el último campo de texto añadido
            binding.layoutAreasDinamicas.removeViewAt(numeroActualDeAreas - 1)
        } else {
            Toast.makeText(this, "Mínimo de 3 áreas requerido", Toast.LENGTH_SHORT).show()
        }
    }

    // --- LÓGICA DE GUARDADO EN FIREBASE (AHORA ES DINÁMICA) ---

    private fun crearUsuarioYAreas() {
        val user = mAuth.currentUser
        if (user == null) {
            // Error: No hay usuario. Regresar al Login.
            Toast.makeText(this, "Error: No se encontró usuario.", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // 1. Recolectar los nombres de las áreas DINÁMICAMENTE
        val nombresAreas = mutableListOf<String>()
        for (i in 0 until binding.layoutAreasDinamicas.childCount) {
            val view = binding.layoutAreasDinamicas.getChildAt(i)
            if (view is EditText) {
                val textoArea = view.text.toString().trim()

                // 2. Validar que no esté vacío
                if (textoArea.isEmpty()) {
                    Toast.makeText(this, "Por favor, completa todas las áreas", Toast.LENGTH_SHORT).show()
                    binding.btnConfirmarAreas.isEnabled = true // Reactivar botón
                    view.error = "Este campo no puede estar vacío" // Marcar el campo
                    return
                }
                nombresAreas.add(textoArea)
            }
        }

        // 3. Crear el documento principal del usuario
        val userData = hashMapOf(
            "nombre" to user.displayName,
            "email" to user.email,
            "avatar_hp" to 1000L,
            "monedas" to 0L
        )

        // 4. Preparar la operación "Batch" (Lote)
        val batch = db.batch()
        val userDocRef = db.collection("users").document(user.uid)
        batch.set(userDocRef, userData) // Añade la creación del usuario

        // 5. Preparar las Áreas para guardar (usando la lista dinámica)
        for (nombre in nombresAreas) {
            val areaData = hashMapOf(
                "nombre_area" to nombre,
                "nivel" to 1L,
                "xp" to 0L
            )
            val areaDocRef = userDocRef.collection("areas").document()
            batch.set(areaDocRef, areaData) // Añade cada área al lote
        }

        // 6. Ejecutar el lote
        batch.commit()
            .addOnSuccessListener {
                Toast.makeText(this, "¡Bienvenido! Configuración completa.", Toast.LENGTH_LONG).show()
                val intent = Intent(this, InicioActivity::class.java)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
                binding.btnConfirmarAreas.isEnabled = true // Reactivar botón
            }
    }
}