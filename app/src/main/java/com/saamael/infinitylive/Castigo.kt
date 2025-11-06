package com.saamael.infinitylive

import com.google.firebase.firestore.DocumentId

data class Castigo(
    @DocumentId
    var id: String = "",
    var descripcion: String = ""
)