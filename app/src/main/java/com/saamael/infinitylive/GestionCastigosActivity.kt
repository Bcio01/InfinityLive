package com.saamael.infinitylive

import android.os.Bundle
import com.saamael.infinitylive.databinding.ActivityGestionCastigosBinding

class GestionCastigosActivity : BaseActivity() {

    private lateinit var binding: ActivityGestionCastigosBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Infla el layout y lo añade a la BaseActivity
        binding = ActivityGestionCastigosBinding.inflate(layoutInflater)
        baseBinding.contentFrame.addView(binding.root)

        // Activa el botón del menú
        setupMenuButton(binding.btnMenu)
    }
}