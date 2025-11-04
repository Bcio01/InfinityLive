package com.saamael.infinitylive

import com.google.firebase.firestore.DocumentId

// Este es el "molde" para tus Hábitos.
data class Habito(
    @DocumentId
    var id: String = "",

    var descripcion: String = "",
    var es_positivo: Boolean = true,
    var area_id: String? = null, // El ID del 'Area' al que pertenece

    var xp_ganada: Long = 0L,
    var monedas_ganadas: Long = 0L,
    var hp_perdida: Long = 0L
)