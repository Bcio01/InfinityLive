package com.saamael.infinitylive

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SelectAreaActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_select_area)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Confirmar Areas
        val btnAcceder = findViewById<Button>(R.id.btnConfirmarAreas)
        // escuchar el evento
        btnAcceder.setOnClickListener {
            // instancia de Intent y pasar la Clase LoginActivity como parametro
            val intent = Intent(this, InicioActivity::class.java)
            startActivity(intent)
        }
    }
}