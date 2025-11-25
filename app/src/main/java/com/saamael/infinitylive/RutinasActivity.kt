package com.saamael.infinitylive

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.saamael.infinitylive.databinding.ActivityRutinasBinding
// 1. Imports de Firebase
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class RutinasActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRutinasBinding
    // 2. Variable para la referencia a la base de datos
    private lateinit var databaseRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRutinasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 3. Inicializar la referencia.
        // IMPORTANTE: "casa/led" debe ser la misma ruta que pusiste en el código de Arduino
        databaseRef = FirebaseDatabase.getInstance().getReference("casa/led")

        setupBottomMenu()
        setupLedButtons()
    }

    private fun setupLedButtons() {
        binding.btnLedOn.setOnClickListener {
            // Enviar "ON" a Firebase
            databaseRef.setValue("ON")
                .addOnSuccessListener {
                    Toast.makeText(this, "LED Encendido", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        binding.btnLedOff.setOnClickListener {
            // Enviar "OFF" a Firebase
            databaseRef.setValue("OFF")
                .addOnSuccessListener {
                    Toast.makeText(this, "LED Apagado", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
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

        // 2. Icono Diarias
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