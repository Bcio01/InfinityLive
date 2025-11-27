package com.saamael.infinitylive

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
// Nota: 'db' la heredamos de BaseActivity, no hace falta declararla aquí.

class SocialActivity : BaseActivity() {

    // Vistas
    private lateinit var rvAmigos: RecyclerView
    private lateinit var rvSolicitudes: RecyclerView
    private lateinit var tvHeaderSolicitudes: TextView
    private lateinit var fabAgregarAmigo: FloatingActionButton

    // Datos y Adaptadores
    private val listaAmigos = mutableListOf<UsuarioPublico>()
    private val listaSolicitudes = mutableListOf<UsuarioPublico>()

    private lateinit var adapterAmigos: AmigosAdapter
    private lateinit var adapterSolicitudes: SolicitudesAdapter

    // Usuario actual
    private val currentUser = FirebaseAuth.getInstance().currentUser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_social)

        // 1. Vincular Vistas
        rvAmigos = findViewById(R.id.rvAmigos)
        rvSolicitudes = findViewById(R.id.rvSolicitudes)
        tvHeaderSolicitudes = findViewById(R.id.tvHeaderSolicitudes)
        fabAgregarAmigo = findViewById(R.id.fabAgregarAmigo)

        // 2. CONFIGURAR EL MENÚ INFERIOR (NUEVO)
        setupBottomMenu()

        // 3. Configurar RecyclerViews
        rvAmigos.layoutManager = LinearLayoutManager(this)
        rvSolicitudes.layoutManager = LinearLayoutManager(this)

        // 4. Inicializar Adaptadores
        adapterAmigos = AmigosAdapter(listaAmigos) { amigo ->
            mostrarDialogoDetalle(amigo)
        }
        rvAmigos.adapter = adapterAmigos

        adapterSolicitudes = SolicitudesAdapter(
            listaSolicitudes,
            onAceptar = { usuario -> aceptarSolicitud(usuario) },
            onRechazar = { usuario -> rechazarSolicitud(usuario) }
        )
        rvSolicitudes.adapter = adapterSolicitudes

        // 5. Botón flotante
        fabAgregarAmigo.setOnClickListener {
            mostrarDialogoEnviarSolicitud()
        }

        // 6. Cargar datos
        cargarAmigos()
        cargarSolicitudes()
    }

    // --- LÓGICA DEL MENÚ INFERIOR (Adaptada de InicioActivity) ---
    private fun setupBottomMenu() {
        // Referencias a los contenedores del menú (Ids vienen del include bottom_menu.xml)
        val menuHabitos = findViewById<LinearLayout>(R.id.menuHabitos) // Botón Inicio
        val menuDiarias = findViewById<LinearLayout>(R.id.menuDiarias) // Botón Rutinas
        val menuSocial = findViewById<LinearLayout>(R.id.menuSocial)   // Botón Social (Aquí)
        val menuRecompensas = findViewById<LinearLayout>(R.id.menuRecompensas) // Botón Tienda
        val fabCentral = findViewById<View>(R.id.fabDiamondContainer)  // Diamante central

        // 1. Configurar Clicks

        // -> Ir a INICIO
        menuHabitos.setOnClickListener {
            val intent = Intent(this, InicioActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(intent)
            overridePendingTransition(0, 0) // Transición suave
        }

        // -> Ir a RUTINAS
        menuDiarias.setOnClickListener {
            val intent = Intent(this, RutinasActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(intent)
            overridePendingTransition(0, 0)
        }

        // -> SOCIAL (Ya estamos aquí, no hacemos nada o mostramos un Toast)
        menuSocial.setOnClickListener {
            // Opcional: Scroll al inicio de la lista de amigos
            // rvAmigos.smoothScrollToPosition(0)
        }

        // -> Ir a TIENDA
        menuRecompensas.setOnClickListener {
            val intent = Intent(this, TiendaActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(intent)
            overridePendingTransition(0, 0)
        }

        // -> Ir a GESTIÓN DE HÁBITOS (Botón central)
        fabCentral.setOnClickListener {
            val intent = Intent(this, GestionHabitosActivity::class.java)
            startActivity(intent)
        }

        // 2. CAMBIAR EL COLOR VISUAL (Para saber que estamos en "Social")
        // Buscamos el icono y texto de Social
        val iconSocial = findViewById<ImageView>(R.id.iconSocial)
        val textSocial = findViewById<TextView>(R.id.textSocial)

        // Usamos tu color "achievement" (morado) o "acent" (cyan)
        val colorActivo = ContextCompat.getColor(this, R.color.achievement)

        iconSocial.setColorFilter(colorActivo)
        textSocial.setTextColor(colorActivo)

        // Opcional: Si tienes un icono "relleno" para social, úsalo aquí:
        // iconSocial.setImageResource(R.drawable.iconsocial)
    }

    // --- FUNCIONES DE LÓGICA SOCIAL (Ya existentes) ---

    private fun cargarAmigos() {
        if (currentUser == null) return

        db.collection("usuarios").document(currentUser.uid)
            .collection("lista_amigos")
            .addSnapshotListener { snapshots, error ->
                if (error != null || snapshots == null) return@addSnapshotListener

                val idsAmigos = snapshots.documents.map { it.id }

                if (idsAmigos.isEmpty()) {
                    listaAmigos.clear()
                    adapterAmigos.notifyDataSetChanged()
                    return@addSnapshotListener
                }

                listaAmigos.clear()
                var procesados = 0

                for (idAmigo in idsAmigos) {
                    db.collection("usuarios").document(idAmigo).get()
                        .addOnSuccessListener { doc ->
                            val amigo = doc.toObject(UsuarioPublico::class.java)
                            if (amigo != null) {
                                listaAmigos.removeIf { it.id == amigo.id }
                                listaAmigos.add(amigo)
                            }
                            procesados++
                            if (procesados == idsAmigos.size) {
                                listaAmigos.sortByDescending { it.nivelGlobal }
                                adapterAmigos.notifyDataSetChanged()
                            } else {
                                adapterAmigos.notifyDataSetChanged()
                            }
                        }
                }
            }
    }

    private fun cargarSolicitudes() {
        if (currentUser == null) return

        db.collection("usuarios").document(currentUser.uid)
            .collection("solicitudes_recibidas")
            .addSnapshotListener { snapshots, error ->
                if (error != null || snapshots == null) return@addSnapshotListener

                val idsSolicitantes = snapshots.documents.map { it.id }

                if (idsSolicitantes.isNotEmpty()) {
                    tvHeaderSolicitudes.visibility = View.VISIBLE
                    rvSolicitudes.visibility = View.VISIBLE

                    listaSolicitudes.clear()
                    idsSolicitantes.forEach { id ->
                        db.collection("usuarios").document(id).get()
                            .addOnSuccessListener { doc ->
                                val user = doc.toObject(UsuarioPublico::class.java)
                                if (user != null) {
                                    listaSolicitudes.removeIf { it.id == user.id }
                                    listaSolicitudes.add(user)
                                    adapterSolicitudes.notifyDataSetChanged()
                                }
                            }
                    }
                } else {
                    tvHeaderSolicitudes.visibility = View.GONE
                    rvSolicitudes.visibility = View.GONE
                    listaSolicitudes.clear()
                    adapterSolicitudes.notifyDataSetChanged()
                }
            }
    }

    private fun mostrarDialogoEnviarSolicitud() {
        val input = EditText(this)
        input.hint = "Correo exacto del usuario"

        AlertDialog.Builder(this)
            .setTitle("Enviar Solicitud")
            .setView(input)
            .setPositiveButton("Enviar") { _, _ ->
                val correo = input.text.toString().trim()
                if (correo.isNotEmpty()) enviarSolicitud(correo)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun enviarSolicitud(correo: String) {
        if (currentUser == null) return

        db.collection("usuarios")
            .whereEqualTo("correo", correo)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(this, "Usuario no encontrado", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val usuarioDestino = documents.documents[0]
                val idDestino = usuarioDestino.id

                if (idDestino == currentUser.uid) {
                    Toast.makeText(this, "No puedes enviarte solicitud a ti mismo", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val solicitudData = hashMapOf(
                    "timestamp" to FieldValue.serverTimestamp(),
                    "desde_id" to currentUser.uid
                )

                db.collection("usuarios").document(idDestino)
                    .collection("solicitudes_recibidas").document(currentUser.uid)
                    .set(solicitudData)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Solicitud enviada", Toast.LENGTH_SHORT).show()
                    }
            }
    }

    private fun aceptarSolicitud(nuevoAmigo: UsuarioPublico) {
        if (currentUser == null) return

        val batch = db.batch()
        val myId = currentUser.uid
        val friendId = nuevoAmigo.id

        val yoRef = db.collection("usuarios").document(myId)
        val amigoRef = db.collection("usuarios").document(friendId)

        val dataAmistad = hashMapOf("desde" to FieldValue.serverTimestamp())
        batch.set(yoRef.collection("lista_amigos").document(friendId), dataAmistad)
        batch.set(amigoRef.collection("lista_amigos").document(myId), dataAmistad)
        batch.delete(yoRef.collection("solicitudes_recibidas").document(friendId))

        batch.commit().addOnSuccessListener {
            Toast.makeText(this, "¡Ahora son amigos!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun rechazarSolicitud(usuario: UsuarioPublico) {
        if (currentUser == null) return
        db.collection("usuarios").document(currentUser.uid)
            .collection("solicitudes_recibidas").document(usuario.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Solicitud rechazada", Toast.LENGTH_SHORT).show()
            }
    }

    private fun mostrarDialogoDetalle(amigo: UsuarioPublico) {
        AlertDialog.Builder(this)
            .setTitle(amigo.nombre)
            .setMessage("Nivel Global: ${amigo.nivelGlobal}\nCorreo: ${amigo.correo}")
            .setPositiveButton("Cerrar", null)
            .setNeutralButton("Eliminar Amigo") { _, _ ->
                AlertDialog.Builder(this)
                    .setTitle("¿Estás seguro?")
                    .setMessage("Vas a eliminar a ${amigo.nombre} de tu lista de amigos.")
                    .setPositiveButton("Sí, eliminar") { _, _ ->
                        eliminarAmigo(amigo)
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
            .show()
    }

    private fun eliminarAmigo(amigo: UsuarioPublico) {
        if (currentUser == null) return

        val batch = db.batch()

        val yoRef = db.collection("usuarios").document(currentUser.uid)
            .collection("lista_amigos").document(amigo.id)

        val elRef = db.collection("usuarios").document(amigo.id)
            .collection("lista_amigos").document(currentUser.uid)

        batch.delete(yoRef)
        batch.delete(elRef)

        batch.commit()
            .addOnSuccessListener {
                Toast.makeText(this, "Eliminado de tus amigos", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al eliminar", Toast.LENGTH_SHORT).show()
            }
    }
}