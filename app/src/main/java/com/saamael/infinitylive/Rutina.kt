package com.saamael.infinitylive

data class Rutina(
    var id: String = "",
    var hora: Int = 0,
    var minuto: Int = 0,
    var tipo: String = "LUZ", // "LUZ" o "BUZZER"
    var activa: Boolean = true
)