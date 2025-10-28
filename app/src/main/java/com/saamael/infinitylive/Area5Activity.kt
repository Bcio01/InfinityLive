package com.saamael.infinitylive

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout

class Area5Activity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_area5)

        // Menu
        drawerLayout = findViewById(R.id.drawerLayout)
        val btnMenu = findViewById<Button>(R.id.btnMenu)
        val sideMenuScroll = findViewById<android.widget.ScrollView>(R.id.sideMenuScroll)

        btnMenu.setOnClickListener {
            if (drawerLayout.isDrawerOpen(sideMenuScroll)) {
                drawerLayout.closeDrawer(sideMenuScroll)
            } else {
                drawerLayout.openDrawer(sideMenuScroll)
            }
        }

        // inicio
        val inicio = findViewById<LinearLayout>(R.id.inicioMenu)

        inicio.setOnClickListener {
            val intent = Intent(this, InicioActivity::class.java)
            startActivity(intent)
        }

        // sub menu areas
        val areasMenu = findViewById<LinearLayout>(R.id.areasMenu)
        val subAreasMenu = findViewById<LinearLayout>(R.id.subMenuAreas)

        areasMenu.setOnClickListener {
            if (subAreasMenu.visibility == View.VISIBLE){
                subAreasMenu.visibility = View.GONE
            } else {
                subAreasMenu.visibility = View.VISIBLE
            }
        }

        val area1 = findViewById<TextView>(R.id.area1)
        val area2 = findViewById<TextView>(R.id.area2)
        val area3 = findViewById<TextView>(R.id.area3)
        val area4 = findViewById<TextView>(R.id.area4)
        val area5 = findViewById<TextView>(R.id.area5)

        // area 1
        area1.setOnClickListener {
            val intent = Intent(this, Area1Activity::class.java)
            startActivity(intent)
        }
        // area 2
        area2.setOnClickListener {
            val intent = Intent(this, Area2Activity::class.java)
            startActivity(intent)
        }
        // area 3
        area3.setOnClickListener {
            val intent = Intent(this, Area3Activity::class.java)
            startActivity(intent)
        }
        // area 4
        area4.setOnClickListener {
            val intent = Intent(this, Area4Activity::class.java)
            startActivity(intent)
        }
        // area 5
        area5.setOnClickListener {
            val intent = Intent(this, Area5Activity::class.java)
            startActivity(intent)
        }

        // Tienda
        val tienda = findViewById<LinearLayout>(R.id.tiendaMenu)

        tienda.setOnClickListener {
            intent = Intent(this, TiendaActivity::class.java)
            startActivity(intent)
        }

        // logout
        val logout = findViewById<LinearLayout>(R.id.logoutMenu)
        logout.setOnClickListener {
            intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }
}