package com.saamael.infinitylive

import android.os.Bundle
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.saamael.infinitylive.databinding.ActivityCrearProductoBinding

class CrearProductoActivity : BaseActivity() {

    private lateinit var binding: ActivityCrearProductoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCrearProductoBinding.inflate(layoutInflater)
        // Aquí no usamos el menú lateral porque es una sub-pantalla
        setContentView(binding.root)

        binding.btnGuardarProducto.setOnClickListener {
            guardarProducto()
        }
    }

    private fun guardarProducto() {
        val nombre = binding.etNombreProducto.text.toString().trim()
        val desc = binding.etDescProducto.text.toString().trim()
        val precioStr = binding.etPrecioProducto.text.toString().trim()

        if (nombre.isEmpty() || precioStr.isEmpty()) {
            Toast.makeText(this, "Completa el nombre y el precio", Toast.LENGTH_SHORT).show()
            return
        }

        val nuevoItem = ShopItem(
            nombre = nombre,
            descripcion = desc,
            precio = precioStr.toInt(),
            iconName = "custom" // Marca para saber que es personalizado
        )

        if (uid != null) {
            db.collection("users").document(uid!!)
                .collection("items_tienda") // Guardamos en una colección nueva
                .add(nuevoItem)
                .addOnSuccessListener {
                    Toast.makeText(this, "Recompensa creada", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al guardar", Toast.LENGTH_SHORT).show()
                }
        }
    }
}