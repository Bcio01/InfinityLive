package com.saamael.infinitylive // Asegúrate de que este sea tu paquete

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.saamael.infinitylive.databinding.ActivitySelectAreaBinding // Importa tu ViewBinding

class SelectAreaActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySelectAreaBinding
    private lateinit var mAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelectAreaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar Firebase
        mAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Asignar el clic al botón
        binding.btnConfirmarAreas.setOnClickListener {
            // Desactivar el botón para evitar clics duplicados
            it.isEnabled = false

            // Llamar a la función para guardar todo
            crearUsuarioYAreas()
        }
    }

    private fun crearUsuarioYAreas() {
        val user = mAuth.currentUser
        if (user == null) {
            // Si el usuario es nulo, algo salió muy mal. Regresarlo al Login.
            Toast.makeText(this, "Error: No se encontró usuario.", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // 1. Recolectar los nombres de las 9 áreas
        val nombresAreas = listOf(
            binding.etArea1.text.toString().trim(),
            binding.etArea2.text.toString().trim(),
            binding.etArea3.text.toString().trim(),
            binding.etArea4.text.toString().trim(),
            binding.etArea5.text.toString().trim(),
            binding.etArea6.text.toString().trim(),
            binding.etArea7.text.toString().trim(),
            binding.etArea8.text.toString().trim(),
            binding.etArea9.text.toString().trim()
        )

        // 2. Validar que ningún campo esté vacío
        if (nombresAreas.any { it.isEmpty() }) {
            Toast.makeText(this, "Por favor, completa las 9 áreas", Toast.LENGTH_SHORT).show()
            binding.btnConfirmarAreas.isEnabled = true // Reactivar el botón
            return
        }

        // 3. Crear el documento principal del usuario
        val userData = hashMapOf(
            "nombre" to user.displayName,
            "email" to user.email,
            "avatar_hp" to 1000L, // 'L' para indicar que es un número Long (importante en Firestore)
            "monedas" to 0L
        )

        // 4. Preparar la operación "Batch" (Lote)
        // Esto nos permite guardar el usuario Y las 9 áreas en una sola operación atómica.
        // Si algo falla, no se guarda nada.
        val batch = db.batch()

        // Referencia al documento del usuario: /users/{ID_DEL_USUARIO}
        val userDocRef = db.collection("users").document(user.uid)

        // Añadir la creación del usuario al lote
        batch.set(userDocRef, userData)

        // 5. Preparar las 9 áreas para guardar
        for (nombre in nombresAreas) {
            val areaData = hashMapOf(
                "nombre_area" to nombre,
                "nivel" to 1L,
                "xp" to 0L
            )
            // Referencia a un *nuevo* documento dentro de la sub-colección de áreas
            // /users/{ID_DEL_USUARIO}/areas/{ID_DE_AREA_ALEATORIO}
            val areaDocRef = userDocRef.collection("areas").document()

            // Añadir la creación del área al lote
            batch.set(areaDocRef, areaData)
        }

        // 6. Ejecutar el lote
        batch.commit()
            .addOnSuccessListener {
                // ¡Éxito! Todo se guardó correctamente
                Toast.makeText(this, "¡Bienvenido! Tu configuración está completa.", Toast.LENGTH_LONG).show()

                // Enviar al usuario a la pantalla principal
                val intent = Intent(this, InicioActivity::class.java)
                startActivity(intent)
                finish() // Terminar esta actividad
            }
            .addOnFailureListener { e ->
                // Ocurrió un error
                Toast.makeText(this, "Error al guardar la configuración: ${e.message}", Toast.LENGTH_LONG).show()
                binding.btnConfirmarAreas.isEnabled = true // Reactivar el botón
            }
    }
}