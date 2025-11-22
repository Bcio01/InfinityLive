package com.saamael.infinitylive

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.saamael.infinitylive.databinding.ActivitySelectAreaBinding

class SelectAreaActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySelectAreaBinding
    private lateinit var mAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Definimos el mínimo requerido según tu texto en el XML
    private val MIN_AREAS = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelectAreaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // El botón confirmar ahora recolecta la información de los botones seleccionados
        binding.btnConfirmarAreas.setOnClickListener {
            crearUsuarioYAreas()
        }
    }

    private fun crearUsuarioYAreas() {
        // Desactivar botón para evitar múltiples clics mientras carga
        binding.btnConfirmarAreas.isEnabled = false

        val user = mAuth.currentUser
        if (user == null) {
            Toast.makeText(this, "Error: No se encontró usuario.", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // 1. RECOLECTAR ÁREAS SELECCIONADAS
        // Creamos una lista con todos los botones de la vista para iterar fácilmente
        val listaBotones = listOf(
            binding.btnArea1,
            binding.btnArea2,
            binding.btnArea3,
            binding.btnArea4,
            binding.btnArea5,
            binding.btnArea6
        )

        val nombresAreasSeleccionadas = mutableListOf<String>()

        // Revisamos cuál está marcado (isChecked gracias al XML)
        for (boton in listaBotones) {
            if (boton.isChecked) {
                nombresAreasSeleccionadas.add(boton.text.toString())
            }
        }

        // 2. VALIDACIÓN
        if (nombresAreasSeleccionadas.size < MIN_AREAS) {
            Toast.makeText(this, "Por favor selecciona al menos $MIN_AREAS áreas.", Toast.LENGTH_SHORT).show()
            binding.btnConfirmarAreas.isEnabled = true // Reactivamos el botón
            return
        }

        // 3. PREPARAR DATOS DE USUARIO
        val userData = hashMapOf(
            "nombre" to (user.displayName ?: "Usuario"),
            "email" to (user.email ?: ""),
            "avatar_hp" to 1000L,
            "monedas" to 0L,
            "fecha_registro" to com.google.firebase.Timestamp.now()
        )

        // 4. OPERACIÓN BATCH (LOTE) EN FIREBASE
        val batch = db.batch()
        val userDocRef = db.collection("users").document(user.uid)

        // Guardar datos del usuario
        batch.set(userDocRef, userData)

        // 5. GUARDAR LAS ÁREAS SELECCIONADAS
        // Recorremos la lista de nombres que filtramos arriba
        for (nombreArea in nombresAreasSeleccionadas) {
            // Creamos un ID automático para cada área dentro de la subcolección
            val areaDocRef = userDocRef.collection("areas").document()

            val areaData = hashMapOf(
                "nombre_area" to nombreArea,
                "nivel" to 1L,
                "xp" to 0L,
                "fecha_creacion" to com.google.firebase.Timestamp.now()
            )

            batch.set(areaDocRef, areaData)
        }

        // 6. EJECUTAR EL GUARDADO
        batch.commit()
            .addOnSuccessListener {
                Toast.makeText(this, "¡Bienvenido! Configuración completa.", Toast.LENGTH_LONG).show()
                val intent = Intent(this, InicioActivity::class.java)
                // Limpiamos el stack para que no pueda volver atrás con el botón físico
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish() // Cierra esta actividad completamente
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
                binding.btnConfirmarAreas.isEnabled = true // Reactivar botón si falló
            }
    }
}