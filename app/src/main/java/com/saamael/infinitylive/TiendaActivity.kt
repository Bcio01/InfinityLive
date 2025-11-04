package com.saamael.infinitylive

import android.os.Bundle
import androidx.core.content.ContextCompat
import com.saamael.infinitylive.databinding.ActivityTiendaBinding // Se genera de activity_tienda.xml

// 1. Hereda de BaseActivity
class TiendaActivity : BaseActivity() {

    private lateinit var binding: ActivityTiendaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 2. Infla el layout de la tienda y lo añade a la BaseActivity
        binding = ActivityTiendaBinding.inflate(layoutInflater)
        baseBinding.contentFrame.addView(binding.root)

        // 3. Activa el botón del menú
        setupMenuButton(binding.btnMenu)

        // 4. Resalta "Tienda" en el menú
        highlightActiveMenuItem()
    }

    private fun highlightActiveMenuItem() {
        // Pinta el texto de "Tienda" de color verde
        bindingMenu.tvMenuTienda.setTextColor(ContextCompat.getColor(this, R.color.success))
    }
}