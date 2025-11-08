package com.saamael.infinitylive.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

// 1. Definimos constantes para la BD (Buena práctica)
object PerfilContract {
    const val DATABASE_NAME = "infinity_live_local.db"
    const val DATABASE_VERSION = 1

    object Entry {
        const val TABLE_NAME = "perfil"
        const val COLUMN_ID = "id" // Solo tendremos 1 fila, con id = 1
        const val COLUMN_IMAGE_PATH = "path_foto" // Guardaremos la RUTA (TEXT)
        const val COLUMN_BIOGRAFIA = "biografia" // Guardaremos la Bio (TEXT)
    }
}

// 2. Esta es la clase de tu PDF, pero en Kotlin y sin errores
class PerfilDbHelper(context: Context) : SQLiteOpenHelper(
    context,
    PerfilContract.DATABASE_NAME,
    null,
    PerfilContract.DATABASE_VERSION
) {

    // 3. Se ejecuta para crear la tabla por primera vez
    override fun onCreate(db: SQLiteDatabase?) {
        val createTableQuery = """
            CREATE TABLE ${PerfilContract.Entry.TABLE_NAME} (
                ${PerfilContract.Entry.COLUMN_ID} INTEGER PRIMARY KEY,
                ${PerfilContract.Entry.COLUMN_IMAGE_PATH} TEXT,
                ${PerfilContract.Entry.COLUMN_BIOGRAFIA} TEXT
            )
        """.trimIndent()

        db?.execSQL(createTableQuery)

        // CREATE: Insertamos una fila vacía por defecto
        val values = ContentValues().apply {
            put(PerfilContract.Entry.COLUMN_ID, 1)
            put(PerfilContract.Entry.COLUMN_IMAGE_PATH, "")
            put(PerfilContract.Entry.COLUMN_BIOGRAFIA, "")
        }
        db?.insert(PerfilContract.Entry.TABLE_NAME, null, values)
    }

    // 4. Se ejecuta si cambias DATABASE_VERSION
    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS ${PerfilContract.Entry.TABLE_NAME}")
        onCreate(db)
    }
}