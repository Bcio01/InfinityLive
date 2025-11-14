package com.saamael.infinitylive

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.saamael.infinitylive.databinding.ActivityBaseBinding // Se genera de activity_base.xml
import com.saamael.infinitylive.databinding.LayoutSideMenuBinding // Se genera de layout_side_menu.xml
import com.saamael.infinitylive.db.PerfilContract
import com.saamael.infinitylive.db.PerfilDbHelper

// "open" significa que otras clases pueden heredar de esta
open class BaseActivity : AppCompatActivity() {

    // Hacemos 'baseBinding' PROTECTED para que las clases hijas puedan acceder al content_frame
    protected lateinit var baseBinding: ActivityBaseBinding
    protected lateinit var bindingMenu: LayoutSideMenuBinding

    protected lateinit var mAuth: FirebaseAuth
    protected lateinit var db: FirebaseFirestore
    protected var uid: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflar el "cascarón" (activity_base.xml)
        baseBinding = ActivityBaseBinding.inflate(layoutInflater)
        // Inflar el menú (layout_side_menu.xml)
        bindingMenu = LayoutSideMenuBinding.bind(baseBinding.root.findViewById(R.id.sideMenuScroll))

        // Establecer el layout de la BaseActivity como el principal
        setContentView(baseBinding.root)

        // Inicializar Firebase
        mAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        uid = mAuth.currentUser?.uid

        // Configurar la navegación
        setupDrawerNavigation()
        cargarDatosDelMenu()
    }

    override fun onResume() {
        super.onResume()
        // Vuelve a cargar la foto del menú CADA VEZ que la actividad se muestre
        cargarDatosDelMenu()
    }

    // Pega esta nueva función en BaseActivity.kt
    // (Reemplaza la antigua 'cargarFotoPerfilMenu')
    protected fun cargarDatosDelMenu() {
        // 1. Cargar Nombre de Firebase Auth (es lo más rápido)
        val currentUser = mAuth.currentUser
        if (currentUser != null && currentUser.displayName != null && currentUser.displayName!!.isNotEmpty()) {
            bindingMenu.tvUserName.text = currentUser.displayName
        } else {
            // Si Auth no tiene el nombre, búscalo en Firestore (plan B)
            if (uid != null) {
                db.collection("users").document(uid!!).get().addOnSuccessListener { snapshot ->
                    if (snapshot != null && snapshot.exists()) {
                        bindingMenu.tvUserName.text = snapshot.getString("nombre")
                    }
                }
            } else {
                bindingMenu.tvUserName.text = "Usuario" // Fallback
            }
        }


        if (uid == null) return // Si no hay usuario, no hay nada que buscar

        val dbHelper = PerfilDbHelper(this)
        val db = dbHelper.readableDatabase

        // Buscamos la fila específica de este usuario
        val cursor: android.database.Cursor = db.rawQuery(
            "SELECT * FROM ${PerfilContract.Entry.TABLE_NAME} WHERE ${PerfilContract.Entry.COLUMN_USER_UID} = ?",
            arrayOf(uid) // <-- Argumento de selección
        )

        if (cursor.moveToFirst()) {
            val pathFoto = cursor.getString(cursor.getColumnIndexOrThrow(PerfilContract.Entry.COLUMN_IMAGE_PATH))
            if (!pathFoto.isNullOrEmpty()) {
                com.bumptech.glide.Glide.with(this)
                    .load(java.io.File(pathFoto))
                    .circleCrop()
                    .into(bindingMenu.imgUserIcon)
            } else {
                bindingMenu.imgUserIcon.setImageResource(R.drawable.usericon)
            }
        } else {
            // Si no hay fila para este usuario, muestra la foto por defecto
            bindingMenu.imgUserIcon.setImageResource(R.drawable.usericon)
        }
        cursor.close()
    }

    private fun setupDrawerNavigation() {
        // --- Clics de navegación estáticos ---

        bindingMenu.userMenu.setOnClickListener {
            val intent = Intent(this, PerfilActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(intent)
            baseBinding.drawerLayout.closeDrawer(GravityCompat.START)
        }


        bindingMenu.inicioMenu.setOnClickListener {
            val intent = Intent(this, InicioActivity::class.java)
            // --- AÑADE ESTA LÍNEA ---
            // Esto evita crear una actividad nueva si ya existe
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            // ---
            startActivity(intent)
            baseBinding.drawerLayout.closeDrawer(GravityCompat.START)
        }


        bindingMenu.tiendaMenu.setOnClickListener {
            // TODO: Crear TiendaActivity
            val intent = Intent(this, TiendaActivity::class.java)
            // --- AÑADE ESTA LÍNEA ---
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            // ---
            startActivity(intent)
            // Toast.makeText(this, "Tienda (próximamente)", Toast.LENGTH_SHORT).show()
            baseBinding.drawerLayout.closeDrawer(GravityCompat.START)
        }

        // Clic en el TÍTULO "Hábitos y Castigos" (para expandir)
        bindingMenu.habitosMenu.setOnClickListener {
            if (bindingMenu.subMenuHabitos.visibility == View.GONE) {
                bindingMenu.subMenuHabitos.visibility = View.VISIBLE
            } else {
                bindingMenu.subMenuHabitos.visibility = View.GONE
            }
        }

        // Clic en la opción "Añadir Hábito"
                bindingMenu.tvMenuAnadirHabito.setOnClickListener {
                    val intent = Intent(this, GestionHabitosActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    startActivity(intent)
                    baseBinding.drawerLayout.closeDrawer(GravityCompat.START)
                }

        // Clic en la opción "Gestionar Castigos"
                bindingMenu.tvMenuGestionarCastigos.setOnClickListener {
                    // TODO: Crearemos esta actividad en la rama "funcionalidad/muerte-y-castigos"

                    // Por ahora, creamos el intent y la actividad vacía para que no crashee
                    val intent = Intent(this, GestionCastigosActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    startActivity(intent)
                    baseBinding.drawerLayout.closeDrawer(GravityCompat.START)
                }

        bindingMenu.logoutMenu.setOnClickListener {
            mAuth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        bindingMenu.areasMenu.setOnClickListener {
            // Expandir/colapsar el submenú
            if (bindingMenu.subMenuAreas.visibility == View.GONE) {
                bindingMenu.subMenuAreas.visibility = View.VISIBLE
            } else {
                bindingMenu.subMenuAreas.visibility = View.GONE
            }
        }

        populateAreasSubMenu()
    }


    private fun populateAreasSubMenu() {
        if (uid == null) return // No hay usuario, no cargar nada

        db.collection("users").document(uid!!)
            .collection("areas")
            .get()
            .addOnSuccessListener { snapshot ->
                bindingMenu.subMenuAreas.removeAllViews() // Limpiar menú anterior

                for (document in snapshot.documents) {
                    val area = document.toObject(Area::class.java)
                    if (area != null) {
                        val textView = TextView(this)
                        textView.text = area.nombre_area
                        textView.setPadding(40, 20, 40, 20)
                        textView.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
                        textView.textSize = 14f

                        textView.setOnClickListener {
                            // TODO: Crear AreaDetailActivity
                            Toast.makeText(this, "Viendo ${area.nombre_area}", Toast.LENGTH_SHORT).show()
                            baseBinding.drawerLayout.closeDrawer(GravityCompat.START)
                        }
                        bindingMenu.subMenuAreas.addView(textView)
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar áreas del menú", Toast.LENGTH_SHORT).show()
            }
    }

    // --- Función para que el botón ☰ abra el menú ---
    // La clase hija (InicioActivity) debe llamar a esto
    protected fun setupMenuButton(btnMenu: View) {
        btnMenu.setOnClickListener {
            if (baseBinding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                baseBinding.drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                baseBinding.drawerLayout.openDrawer(GravityCompat.START)
            }
        }
    }
}