package com.saamael.infinitylive

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.saamael.infinitylive.databinding.ActivityMainBinding // Importa el ViewBinding

class MainActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var binding: ActivityMainBinding // Declara la variable de binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar Firebase Auth
        mAuth = FirebaseAuth.getInstance()

        // Comprueba si el usuario ya inició sesión
        if (mAuth.currentUser != null) {
            // Si ya hay sesión, salta directo al Inicio
            goToInicioActivity()
            return // Evita que se ejecute el resto del onCreate
        }

        // Si no hay sesión, infla y muestra la pantalla de bienvenida
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root) // Usa la raíz del binding

        // Asigna el clic usando el binding
        binding.btnComenzar.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }

    private fun goToInicioActivity() {
        val intent = Intent(this, InicioActivity::class.java)
        // Estos flags borran el historial y crean una nueva tarea
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish() // Cierra esta MainActivity
    }
}