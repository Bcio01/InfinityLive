package com.saamael.infinitylive

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
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
        // A. Icono Inicio
        binding.MenuInferior.menuHabitos.setOnClickListener {
            val intent = Intent(this, InicioActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        // 2. Icono Diarias
        binding.MenuInferior.menuDiarias.setOnClickListener {
            Toast.makeText(this, "Ya estás en Rutinas", Toast.LENGTH_SHORT).show()
        }

        // C. BOTÓN DIAMANTE (NUEVA LÓGICA DE AGREGAR)
        binding.MenuInferior.fabDiamondContainer.setOnClickListener {
            mostrarDialogoAgregar()
        }

        // D. Icono Social
        binding.MenuInferior.menuPendientes.setOnClickListener {
            Toast.makeText(this, "Próximamente", Toast.LENGTH_SHORT).show()
        }

        // E. Icono Tienda
        binding.MenuInferior.menuRecompensas.setOnClickListener {
            val intent = Intent(this, TiendaActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(intent)
        }
    }

    private fun configurarRecycler() {
        adapter = RutinasAdapter(listaRutinas,
            onBorrarClick = { rutina ->
                // Borrar de Firebase
                if (rutina.id.isNotEmpty()) {
                    databaseRef.child(rutina.id).removeValue()
                        .addOnFailureListener {
                            Toast.makeText(this, "Error al borrar", Toast.LENGTH_SHORT).show()
                        }
                }
            },
            onSwitchChange = { rutina, isActive ->
                // Actualizar estado en Firebase
                if (rutina.id.isNotEmpty()) {
                    databaseRef.child(rutina.id).child("activa").setValue(isActive)
                }
            }
        )

        binding.rvRutinas.layoutManager = LinearLayoutManager(this)
        binding.rvRutinas.adapter = adapter
    }

    private fun cargarDatosDeFirebase() {
        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listaRutinas.clear()
                for (data in snapshot.children) {
                    val rutina = data.getValue(Rutina::class.java)
                    if (rutina != null) {
                        rutina.id = data.key ?: ""
                        listaRutinas.add(rutina)
                    }
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@RutinasActivity, "Error al cargar: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun mostrarDialogoAgregar() {
        val calendario = Calendar.getInstance()
        val horaActual = calendario.get(Calendar.HOUR_OF_DAY)
        val minutoActual = calendario.get(Calendar.MINUTE)

        TimePickerDialog(this, { _, hora, minuto ->
            mostrarDialogoTipo(hora, minuto)
        }, horaActual, minutoActual, true).show()
    }

    private fun mostrarDialogoTipo(hora: Int, minuto: Int) {
        val opciones = arrayOf("Luz (LED)", "Alarma (Buzzer)")

        AlertDialog.Builder(this)
            .setTitle("¿Qué acción realizará?")
            .setSingleChoiceItems(opciones, 0) { dialog, which ->
                val tipoSeleccionado = if (which == 0) "LUZ" else "BUZZER"
                guardarRutinaEnFirebase(hora, minuto, tipoSeleccionado)
                dialog.dismiss()
            }
            .show()
    }

    private fun guardarRutinaEnFirebase(hora: Int, minuto: Int, tipo: String) {
        val nuevoId = databaseRef.push().key ?: return

        val nuevaRutina = Rutina(
            id = nuevoId,
            hora = hora,
            minuto = minuto,
            tipo = tipo,
            activa = true
        )

        databaseRef.child(nuevoId).setValue(nuevaRutina)
            .addOnSuccessListener {
                Toast.makeText(this, "Rutina Guardada", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al guardar", Toast.LENGTH_SHORT).show()
            }
    }
}