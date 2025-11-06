package com.saamael.infinitylive

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.Query
import com.saamael.infinitylive.databinding.ActivityGestionCastigosBinding

// 1. Implementa la interfaz del adaptador
class GestionCastigosActivity : BaseActivity(), CastigoAdapter.OnCastigoListener {

    private lateinit var binding: ActivityGestionCastigosBinding
    private var adapter: CastigoAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityGestionCastigosBinding.inflate(layoutInflater)
        baseBinding.contentFrame.addView(binding.root)
        setupMenuButton(binding.btnMenu)

        // 2. Configurar el botón de Añadir
        binding.btnAnadirCastigo.setOnClickListener {
            anadirCastigo()
        }

        // 3. Configurar la lista
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        if (uid == null) return

        // Consulta: Traer todos los castigos del usuario
        val query: Query = db.collection("users").document(uid!!)
            .collection("castigos")
            .orderBy("descripcion")

        val options = FirestoreRecyclerOptions.Builder<Castigo>()
            .setQuery(query, Castigo::class.java)
            .build()

        adapter = CastigoAdapter(options)
        adapter?.listener = this // Conecta el listener de borrado

        binding.rvCastigos.layoutManager = LinearLayoutManager(this)
        binding.rvCastigos.adapter = adapter
    }

    private fun anadirCastigo() {
        if (uid == null) return
        val descripcion = binding.etCastigoDescripcion.text.toString().trim()

        if (descripcion.isEmpty()) {
            binding.etCastigoDescripcion.error = "La descripción no puede estar vacía"
            return
        }

        val castigo = Castigo(descripcion = descripcion)

        // Guardar en la sub-colección "castigos"
        db.collection("users").document(uid!!).collection("castigos")
            .add(castigo)
            .addOnSuccessListener {
                Toast.makeText(this, "Castigo añadido", Toast.LENGTH_SHORT).show()
                binding.etCastigoDescripcion.setText("") // Limpia el campo
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al añadir: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    // 4. Esta función se llama desde el adaptador al presionar "Borrar"
    override fun onBorrarCastigo(id: String) {
        if (uid == null) return

        // Preguntar al usuario para confirmar
        AlertDialog.Builder(this)
            .setTitle("Confirmar Borrado")
            .setMessage("¿Estás seguro de que quieres borrar este castigo?")
            .setPositiveButton("Borrar") { dialog, _ ->
                // Borrar el documento de Firestore
                db.collection("users").document(uid!!).collection("castigos")
                    .document(id)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Castigo borrado", Toast.LENGTH_SHORT).show()
                    }
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    // 5. Iniciar y detener el 'escucha' del adaptador
    override fun onStart() {
        super.onStart()
        adapter?.startListening()
    }

    override fun onStop() {
        super.onStop()
        adapter?.stopListening()
    }
}