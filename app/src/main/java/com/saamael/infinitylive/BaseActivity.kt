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
    }

    // --- ELIMINAMOS setActivityContent() ---
    // La actividad hija se encargará de inflar y añadir su propia vista

    private fun setupDrawerNavigation() {
        // --- Clics de navegación estáticos ---

        // En BaseActivity.kt, dentro de setupDrawerNavigation()
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