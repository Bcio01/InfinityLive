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

// En BaseActivity.kt

    // 1. Añade el onResume para que se actualice al volver de otra pantalla
    override fun onResume() {
        super.onResume()
        cargarDatosDelMenu()
    }

    // 2. Asegúrate de tener esta función completa
    protected fun cargarDatosDelMenu() {
        // A) Cargar Nombre (Firebase)
        val currentUser = mAuth.currentUser
        if (currentUser != null && !currentUser.displayName.isNullOrEmpty()) {
            bindingMenu.tvUserName.text = currentUser.displayName
        } else {
            // Intento de fallback a Firestore si el nombre de Auth es nulo
            if (uid != null) {
                db.collection("users").document(uid!!).get().addOnSuccessListener { snapshot ->
                    if (snapshot != null && snapshot.exists()) {
                        val nombreFirestore = snapshot.getString("nombre")
                        if (!nombreFirestore.isNullOrEmpty()) {
                            bindingMenu.tvUserName.text = nombreFirestore
                        }
                    }
                }
            }
        }

        // B) Cargar Foto (SQLite)
        val dbHelper = com.saamael.infinitylive.db.PerfilDbHelper(this)
        val db = dbHelper.readableDatabase
        // Usamos try-catch para evitar crashes si la tabla no existe aún
        try {
            val cursor = db.rawQuery(
                "SELECT * FROM ${com.saamael.infinitylive.db.PerfilContract.Entry.TABLE_NAME} WHERE ${com.saamael.infinitylive.db.PerfilContract.Entry.COLUMN_USER_UID} = ?",
                arrayOf(uid)
            )

            if (cursor.moveToFirst()) {
                val pathFoto = cursor.getString(cursor.getColumnIndexOrThrow(com.saamael.infinitylive.db.PerfilContract.Entry.COLUMN_IMAGE_PATH))
                if (!pathFoto.isNullOrEmpty()) {
                    com.bumptech.glide.Glide.with(this)
                        .load(java.io.File(pathFoto))
                        .circleCrop()
                        .into(bindingMenu.imgUserIcon)
                } else {
                    bindingMenu.imgUserIcon.setImageResource(R.drawable.usericon)
                }
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // No cerramos la BD para permitir que App Inspection funcione
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