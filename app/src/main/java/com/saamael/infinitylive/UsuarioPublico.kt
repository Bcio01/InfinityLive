package com.saamael.infinitylive

data class UsuarioPublico(
    val id: String = "",
    val nombre: String = "",
    val correo: String = "",
    val nivelGlobal: Int = 1,     // Asegúrate de que se llame así
    val vidaActual: Int = 100,
    val areasResumen: Map<String, Int> = emptyMap() // Vital para mostrar el progreso
)