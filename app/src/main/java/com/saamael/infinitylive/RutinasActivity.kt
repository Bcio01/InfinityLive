package com.saamael.infinitylive

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.saamael.infinitylive.databinding.ActivityRutinasBinding
import com.google.firebase.database.*
import java.util.Calendar

class RutinasActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRutinasBinding
    private lateinit var databaseRef: DatabaseReference

    // Variables para la lista
    private lateinit var adapter: RutinasAdapter
    private val listaRutinas = mutableListOf<Rutina>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRutinasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Referencia a la base de datos
        databaseRef = FirebaseDatabase.getInstance().getReference("casa/rutinas")

        // 2. Configuramos la lista y cargamos datos
        configurarRecycler()
        cargarDatosDeFirebase()

        // 3. Configuramos el menú (Aquí está la lógica del botón Diamante)
        setupBottomMenu()
    }

    private fun setupBottomMenu() {
        // A. Icono Inicio
        binding.MenuInferior.menuHabitos.setOnClickListener {
            val intent = Intent(this, InicioActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        // B. Icono Rutinas (Ya estamos aquí)
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