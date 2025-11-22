package com.saamael.infinitylive

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.saamael.infinitylive.databinding.ActivityRevivirBinding

class RevivirActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRevivirBinding
    private lateinit var mAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var uid: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRevivirBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar Firebase
        mAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        uid = mAuth.currentUser?.uid

        if (uid == null) {
            // Si no hay usuario, no debería estar aquí. Cerrar.
            finish()
            return
        }

        // Cargar un castigo al azar
        cargarCastigoAleatorio()

        // Configurar el botón de revivir
        binding.btnHeCompletado.setOnClickListener {
            revivirAvatar()
        }

        // --- Bloquear el botón de "Atrás" ---
        // Esto evita que el usuario escape del castigo
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Toast.makeText(this@RevivirActivity, "Debes completar tu castigo para continuar", Toast.LENGTH_SHORT).show()
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }

    private fun cargarCastigoAleatorio() {
        db.collection("users").document(uid!!)
            .collection("castigos")
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    // Si el usuario no ha definido castigos
                    binding.tvCastigoAsignado.text = "¡Has tenido suerte! No definiste ningún castigo. (Tu HP se reseteará)"
                } else {
                    // Elige un castigo al azar de la lista
                    val castigos = snapshot.toObjects(Castigo::class.java)
                    val castigoAleatorio = castigos.random()
                    binding.tvCastigoAsignado.text = castigoAleatorio.descripcion
                }
            }
            .addOnFailureListener {
                binding.tvCastigoAsignado.text = "Error al cargar castigos. (Tu HP se reseteará)"
            }
    }

// En RevivirActivity.kt

    private fun revivirAvatar() {
        // Bloquear el botón para evitar doble clic
        binding.btnHeCompletado.isEnabled = false
        binding.btnHeCompletado.text = "Reviviendo..."

        // 1. Calcular la penalización
        val monedasActuales = intent.getLongExtra("MONEDAS_ACTUALES", 0L)
        val penalizacion = (monedasActuales * 0.10).toLong()
        val nuevasMonedas = monedasActuales - penalizacion

        // 2. Lanzamos la actualización a Firebase (NO esperamos respuesta)
        db.collection("users").document(uid!!)
            .update(
                "avatar_hp", 1000L,
                "monedas", nuevasMonedas
            )
        // Ya no hay addOnSuccessListener ni addOnFailureListener aquí

        // 3. ¡AVANZAMOS INMEDIATAMENTE!
        Toast.makeText(this, "¡Has revivido! (Perdiste $penalizacion monedas)", Toast.LENGTH_LONG).show()

        val intent = Intent(this, InicioActivity::class.java)
        // Estos flags aseguran que la pantalla de inicio sea la única que quede
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish() // Cierra la RevivirActivity
    }
}