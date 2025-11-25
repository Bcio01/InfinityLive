package com.saamael.infinitylive

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.saamael.infinitylive.databinding.ActivityRutinasBinding

class RutinasActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRutinasBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRutinasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomMenu()
        setupLedButtons()
    }

    private fun setupLedButtons() {
        binding.btnLedOn.setOnClickListener {
            // AQUÍ IRÁ TU LÓGICA (Bluetooth, WiFi, API request, etc.)
            Toast.makeText(this, "LED Encendido", Toast.LENGTH_SHORT).show()
        }

        binding.btnLedOff.setOnClickListener {
            // AQUÍ IRÁ TU LÓGICA
            Toast.makeText(this, "LED Apagado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupBottomMenu() {
        // 1. Icono Izquierdo (Hábito/Home): Regresa al inicio
        binding.MenuInferior.menuHabitos.setOnClickListener {
            val intent = Intent(this, InicioActivity::class.java)
            // Esto asegura que volvemos a la instancia existente de Inicio sin crear una nueva pila
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish() // Cerramos esta actividad actual
        }

        // 2. Icono Diarias (Podrías dejarlo como 'Próximamente' o que sea esta misma pantalla)
        binding.MenuInferior.menuDiarias.setOnClickListener {
            Toast.makeText(this, "Ya estás en Control", Toast.LENGTH_SHORT).show()
        }

        // 3. Diamante (Gestión de hábitos)
        binding.MenuInferior.fabDiamondContainer.setOnClickListener {
            val intent = Intent(this, GestionHabitosActivity::class.java)
            startActivity(intent)
        }

        // 4. Pendientes
        binding.MenuInferior.menuPendientes.setOnClickListener {
            Toast.makeText(this, "Próximamente", Toast.LENGTH_SHORT).show()
        }

        // 5. Recompensas (Tienda)
        binding.MenuInferior.menuRecompensas.setOnClickListener {
            val intent = Intent(this, TiendaActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(intent)
        }
    }
}