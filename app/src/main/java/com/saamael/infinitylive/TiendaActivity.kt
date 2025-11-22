package com.saamael.infinitylive

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.saamael.infinitylive.databinding.ActivityTiendaBinding

class TiendaActivity : BaseActivity() {

    private lateinit var binding: ActivityTiendaBinding
    private var misMonedas: Long = 0
    private var adapter: TiendaAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityTiendaBinding.inflate(layoutInflater)
        // Añadimos el layout al marco de BaseActivity
        baseBinding.contentFrame.addView(binding.root)

        // Configuración del menú lateral
        setupMenuButton(binding.btnMenu)
        highlightActiveMenuItem()

        if (uid != null) {
            // 1. Escuchar cuántas monedas tiene el usuario
            setupMonedasListener()

            // 2. Configurar la lista de productos
            setupRecyclerView()

            // 3. Crear productos básicos si la tienda está vacía
            crearProductosPorDefecto()

            // 4. Botón para crear nuevos productos (Opcional, si lo añadiste al XML)
            binding.btnCrearProducto.setOnClickListener {
                val intent = Intent(this, CrearProductoActivity::class.java)
                startActivity(intent)
            }
        }
    }

    private fun highlightActiveMenuItem() {
        bindingMenu.tvMenuTienda.setTextColor(ContextCompat.getColor(this, R.color.success))
    }

    private fun setupMonedasListener() {
        db.collection("users").document(uid!!)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                if (snapshot != null && snapshot.exists()) {
                    misMonedas = snapshot.getLong("monedas") ?: 0
                    binding.tvMisMonedas.text = "$misMonedas"
                }
            }
    }

    private fun setupRecyclerView() {
        val query = db.collection("users").document(uid!!)
            .collection("items_tienda")
            .orderBy("precio", Query.Direction.ASCENDING)

        val options = FirestoreRecyclerOptions.Builder<ShopItem>()
            .setQuery(query, ShopItem::class.java)
            .build()

        adapter = TiendaAdapter(options) { item ->
            confirmarCompra(item)
        }

        binding.rvTienda.layoutManager = LinearLayoutManager(this)
        binding.rvTienda.adapter = adapter
        // Desactivar scroll anidado y animaciones para evitar bugs visuales
        binding.rvTienda.isNestedScrollingEnabled = false
        binding.rvTienda.itemAnimator = null
    }

    private fun confirmarCompra(item: ShopItem) {
        if (misMonedas >= item.precio) {
            AlertDialog.Builder(this)
                .setTitle("Confirmar Compra")
                .setMessage("¿Quieres comprar '${item.nombre}' por ${item.precio} monedas?")
                .setPositiveButton("Comprar") { dialog, _ ->
                    procesarCompra(item)
                    dialog.dismiss()
                }
                .setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
                .show()
        } else {
            Toast.makeText(this, "No tienes suficientes monedas (Te faltan ${item.precio - misMonedas})", Toast.LENGTH_SHORT).show()
        }
    }

    private fun procesarCompra(item: ShopItem) {
        val userRef = db.collection("users").document(uid!!)
        val inventarioRef = userRef.collection("inventario")

        // 1. Restar monedas
        userRef.update("monedas", FieldValue.increment(-item.precio.toLong()))
            .addOnSuccessListener {
                // 2. Si se cobró bien, añadir al inventario
                val nuevoItem = hashMapOf(
                    "itemId" to item.id, // ID del producto original
                    "nombre" to item.nombre,
                    "fechaCompra" to FieldValue.serverTimestamp(),
                    "usado" to false
                )

                inventarioRef.add(nuevoItem)
                    .addOnSuccessListener {
                        Toast.makeText(this, "¡Compra exitosa! Añadido a tu inventario.", Toast.LENGTH_LONG).show()
                    }
                    .addOnFailureListener {
                        // En una app real, aquí deberíamos devolver las monedas si falla
                        Toast.makeText(this, "Error al entregar el producto.", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al procesar el pago.", Toast.LENGTH_SHORT).show()
            }
    }

    // Crea productos iniciales solo si la colección está vacía
    private fun crearProductosPorDefecto() {
        val itemsRef = db.collection("users").document(uid!!).collection("items_tienda")

        itemsRef.limit(1).get().addOnSuccessListener { snapshot ->
            if (snapshot.isEmpty) {
                val basicos = listOf(
                    ShopItem("", "Pase Comida Chatarra", "Permite comer algo no saludable.", 100),
                    ShopItem("", "Pase Procrastinación", "5 minutos extra sin culpa.", 50),
                    ShopItem("", "Día Libre", "Un día sin penalizaciones.", 500)
                )
                basicos.forEach { itemsRef.add(it) }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        adapter?.startListening()
    }

    override fun onStop() {
        super.onStop()
        adapter?.stopListening()
    }
}