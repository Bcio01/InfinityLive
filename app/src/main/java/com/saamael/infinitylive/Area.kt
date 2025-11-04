package com.saamael.infinitylive

import com.google.firebase.firestore.DocumentId

// Este es el "molde" para tus Áreas.
data class Area(
    @DocumentId // Esta anotación le dice a Firestore que ponga el ID del documento aquí
    var id: String = "",

    var nombre_area: String = "",
    var nivel: Long = 1L, // Usamos Long para los números en Firestore
    var xp: Long = 0L
)