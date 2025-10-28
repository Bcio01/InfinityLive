package com.saamael.infinitylive

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Login
        val tvRedirigir = findViewById<TextView>(R.id.tvRegistrate)
        // escuchar el evento
        tvRedirigir.setOnClickListener {
            // instancia de Intent y pasar la Clase LoginActivity como parametro
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        // Acceder
        val btnAcceder = findViewById<Button>(R.id.btnAcceder)
        // escuchar el evento
        btnAcceder.setOnClickListener {
            // instancia de Intent y pasar la Clase LoginActivity como parametro
            val intent = Intent(this, SelectAreaActivity::class.java)
            startActivity(intent)
        }
    }
}