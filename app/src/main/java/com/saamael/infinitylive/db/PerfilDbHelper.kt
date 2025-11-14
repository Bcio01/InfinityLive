package com.saamael.infinitylive.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

object PerfilContract {
    const val DATABASE_NAME = "infinity_live_local.db"
    // --- ¡CAMBIO IMPORTANTE! ---
    const val DATABASE_VERSION = 2 // <--- Sube la versión a 2
    // ---

    object Entry {
        const val TABLE_NAME = "perfil"
        // --- ¡CAMBIO IMPORTANTE! ---
        // La clave primaria ahora es el UID de Firebase (TEXTO)
        const val COLUMN_USER_UID = "user_uid"
        // ---
        const val COLUMN_IMAGE_PATH = "path_foto"
        const val COLUMN_BIOGRAFIA = "biografia"
    }
}

class PerfilDbHelper(context: Context) : SQLiteOpenHelper(
    context,
    PerfilContract.DATABASE_NAME,
    null,
    PerfilContract.DATABASE_VERSION
) {

    override fun onCreate(db: SQLiteDatabase?) {
        // --- ¡CAMBIO IMPORTANTE! ---
        // La tabla ahora usa 'user_uid' como Clave Primaria
        val createTableQuery = """
            CREATE TABLE ${PerfilContract.Entry.TABLE_NAME} (
                ${PerfilContract.Entry.COLUMN_USER_UID} TEXT PRIMARY KEY,
                ${PerfilContract.Entry.COLUMN_IMAGE_PATH} TEXT,
                ${PerfilContract.Entry.COLUMN_BIOGRAFIA} TEXT
            )
        """.trimIndent()

        db?.execSQL(createTableQuery)
        // --- Ya no insertamos una fila por defecto ---
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        // Esto se ejecutará porque cambiamos de versión 1 a 2
        // Borra la tabla 'perfil' antigua (con id=1)
        db?.execSQL("DROP TABLE IF EXISTS ${PerfilContract.Entry.TABLE_NAME}")
        // Vuelve a crear la tabla con la nueva estructura (con user_uid)
        onCreate(db)
    }
}