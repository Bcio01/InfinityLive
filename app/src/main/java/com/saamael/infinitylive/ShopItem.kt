package com.saamael.infinitylive

import com.google.firebase.firestore.DocumentId

data class ShopItem(
    @DocumentId
    var id: String = "",
    var nombre: String = "",
    var descripcion: String = "",
    var precio: Int = 0,
    // Usaremos un nombre de icono (String) en lugar de un ID (Int) para evitar errores
    var iconName: String = "default"
)