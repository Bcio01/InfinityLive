package com.saamael.infinitylive

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.saamael.infinitylive.GestionHabitosActivity
import com.saamael.infinitylive.databinding.ActivityTiendaBinding

class TiendaActivity : BaseActivity() {

    private lateinit var binding: ActivityTiendaBinding
    private var misMonedas: Long = 0
    private var adapter: TiendaAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityTiendaBinding.inflate(layoutInflater)
        // CAMBIO 1: Usamos setContentView directo para que el ConstraintLayout ocupe todo
        setContentView(binding.root)

        // CAMBIO 2: Configurar el menú inferior en lugar del lateral
        setupBottomMenu()

        if (uid != null) {
            setupMonedasListener()
            setupRecyclerView()
            crearProductosPorDefecto()

            binding.btnCrearProducto.setOnClickListener {
                val intent = Intent(this, CrearProductoActivity::class.java)
                startActivity(intent)
            }
        }
    }

    // --- NUEVO: Lógica del Menú Inferior ---
    private fun setupBottomMenu() {

        // 1. Botón Inicio (Hábitos) -> Volver a InicioActivity
        binding.MenuInferior.menuHabitos.setOnClickListener {
            val intent = Intent(this, InicioActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(intent)
        }

        // 2. Botón Diamante -> Crear Hábito
        binding.MenuInferior.fabDiamondContainer.setOnClickListener {
            val intent = Intent(this, GestionHabitosActivity::class.java)
            startActivity(intent)
        }

        // 3. Botón Recompensas (Donde estamos) -> Scroll arriba
        binding.MenuInferior.menuRecompensas.setOnClickListener {
            binding.scrollViewTienda.smoothScrollTo(0, 0)
        }

        // 4. Botón Rutinas (NUEVO LINK)
        binding.MenuInferior.menuDiarias.setOnClickListener {
            val intent = Intent(this, RutinasActivity::class.java)
            // FLAG_ACTIVITY_REORDER_TO_FRONT es útil si quieres mantener el estado
            // de RutinasActivity si el usuario va y vuelve. Si no, quítalo.
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(intent)
        }

        // 5. Placeholder
        binding.MenuInferior.menuPendientes.setOnClickListener {
            Toast.makeText(this, "Próximamente", Toast.LENGTH_SHORT).show()
        }
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

        // --- Usamor GridLayoutManager con 3 columnas ---
        binding.rvTienda.layoutManager = androidx.recyclerview.widget.GridLayoutManager(this, 3)

        // binding.rvTienda.layoutManager = LinearLayoutManager(this)
        binding.rvTienda.adapter = adapter
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
            Toast.makeText(this, "No tienes suficientes monedas", Toast.LENGTH_SHORT).show()
        }
    }

    private fun procesarCompra(item: ShopItem) {
        val userRef = db.collection("users").document(uid!!)
        val inventarioRef = userRef.collection("inventario")

        userRef.update("monedas", FieldValue.increment(-item.precio.toLong()))
            .addOnSuccessListener {
                val nuevoItem = hashMapOf(
                    "itemId" to item.id,
                    "nombre" to item.nombre,
                    "fechaCompra" to FieldValue.serverTimestamp(),
                    "usado" to false
                )

                inventarioRef.add(nuevoItem)
                    .addOnSuccessListener {
                        Toast.makeText(this, "¡Comprado!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error al entregar producto.", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al procesar el pago.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun crearProductosPorDefecto() {
        val itemsRef = db.collection("users").document(uid!!).collection("items_tienda")

        itemsRef.limit(1).get().addOnSuccessListener { snapshot ->
            if (snapshot.isEmpty) {
                val basicos = listOf(
                    ShopItem("1", "Pase Comida Chatarra", "Permite comer algo no saludable.", 100),
                    ShopItem("2", "Pase Procrastinación", "5 minutos extra sin culpa.", 50),
                    ShopItem("3", "Día Libre", "Un día sin penalizaciones.", 500),
                    ShopItem("4", "sdlkfj", "Permite comer algo no saludable.", 100),
                    ShopItem("5", "oli item de prueba", "5 minutos extra sin culpa.", 50),
                    ShopItem("6", "probando un item", "Un día sin penalizaciones.", 500),
                    ShopItem("7", "un item random", "Permite comer algo no saludable.", 100),
                    ShopItem("8", "juegate un loto", "5 minutos extra sin culpa.", 50),
                    ShopItem("9", "una escapadita", "Un día sin penalizaciones.", 500)

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