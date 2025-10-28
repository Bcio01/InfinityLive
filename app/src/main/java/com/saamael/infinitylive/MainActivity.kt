package com.saamael.infinitylive

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // traer contenido del layout
        setContentView(R.layout.activity_main)

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // instanciar btn
        val btnComenzar = findViewById<Button>(R.id.btnComenzar)
        // escuchar el evento
        btnComenzar.setOnClickListener {
            // instancia de Intent y pasar la Clase LoginActivity como parametro
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }
}