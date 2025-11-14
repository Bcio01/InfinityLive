package com.saamael.infinitylive

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase // <-- Asegúrate de tener esta importación
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.saamael.infinitylive.db.PerfilContract
import com.saamael.infinitylive.db.PerfilDbHelper
import com.saamael.infinitylive.databinding.ActivityPerfilBinding
import java.io.File

class PerfilActivity : BaseActivity() {

    private lateinit var binding: ActivityPerfilBinding
    private lateinit var dbHelper: PerfilDbHelper
    private var imagenUri: Uri? = null // Guardará la URI de la nueva imagen
    private var imagenPath: String? = null // Guardará la RUTA (String) actual o nueva

    // Lanzador para el resultado de "Elegir Foto"
    private val selectorDeImagen = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            imagenUri = uri
            // Muestra la previsualización de la nueva imagen
            Glide.with(this)
                .load(uri)
                .circleCrop()
                .into(binding.imgPerfil)

            // Convertimos la URI (temporal) a una ruta de archivo (permanente)
            imagenPath = getPathFromUri(this, uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPerfilBinding.inflate(layoutInflater)
        baseBinding.contentFrame.addView(binding.root)
        setupMenuButton(binding.btnMenu)

        // Inicializa el Helper de SQLite
        dbHelper = PerfilDbHelper(this)

        // Carga los datos (si el uid no es nulo)
        if (uid != null) {
            cargarDatosDeFirebase()
            cargarDatosDeSQLite() // CRUD: READ
        }

        // Configura los botones
        binding.btnCambiarFoto.setOnClickListener {
            selectorDeImagen.launch("image/*") // Abre la galería de fotos
        }

        binding.btnGuardarPerfil.setOnClickListener {
            guardarPerfilEnSQLite() // CRUD: UPDATE / CREATE
        }
    }

    private fun cargarDatosDeFirebase() {
        // Muestra los datos de Firebase (no editables)
        val user = mAuth.currentUser
        binding.tvPerfilNombre.text = "Nombre: ${user?.displayName ?: "No disponible"}"
        binding.tvPerfilEmail.text = "Email: ${user?.email ?: "No disponible"}"

        // El HP lo leemos en vivo (igual que en InicioActivity)
        db.collection("users").document(uid!!).get().addOnSuccessListener { doc ->
            val hp = doc.getLong("avatar_hp") ?: 1000L
            binding.tvPerfilHp.text = "HP: $hp / 1000"
        }
    }

    // --- CRUD: READ (MODIFICADO) ---
    private fun cargarDatosDeSQLite() {
        if (uid == null) return // No continuar si no hay uid

        val db = dbHelper.readableDatabase
        val cursor: Cursor = db.rawQuery(
            // Busca la fila que coincida con el UID del usuario actual
            "SELECT * FROM ${PerfilContract.Entry.TABLE_NAME} WHERE ${PerfilContract.Entry.COLUMN_USER_UID} = ?",
            arrayOf(uid) // Argumento de selección
        )

        if (cursor.moveToFirst()) {
            val biografia = cursor.getString(cursor.getColumnIndexOrThrow(PerfilContract.Entry.COLUMN_BIOGRAFIA))
            val pathFoto = cursor.getString(cursor.getColumnIndexOrThrow(PerfilContract.Entry.COLUMN_IMAGE_PATH))

            binding.etBiografia.setText(biografia)

            if (!pathFoto.isNullOrEmpty()) {
                imagenPath = pathFoto // Guarda la ruta actual por si el usuario no la cambia
                Glide.with(this)
                    .load(File(pathFoto)) // Carga desde la ruta del archivo
                    .circleCrop()
                    .into(binding.imgPerfil)
            }
        }
        cursor.close()
        // No cerramos la BD (db.close()) para que App Inspection funcione
    }

    // --- CRUD: UPDATE / CREATE (MODIFICADO) ---
    private fun guardarPerfilEnSQLite() {
        if (uid == null) return // No se puede guardar sin un usuario

        val db = dbHelper.writableDatabase
        val biografia = binding.etBiografia.text.toString()

        // Usamos ContentValues, igual que en tu PDF
        val values = ContentValues().apply {
            put(PerfilContract.Entry.COLUMN_USER_UID, uid) // <-- CLAVE: Usamos el UID
            put(PerfilContract.Entry.COLUMN_BIOGRAFIA, biografia)

            // Si 'imagenPath' no es nulo, significa que (A) cargó la foto antigua o (B) seleccionó una nueva.
            // En ambos casos, guardamos el valor que tenga.
            if (imagenPath != null) {
                put(PerfilContract.Entry.COLUMN_IMAGE_PATH, imagenPath)
            } else {
                // Si el usuario no tenía foto y no eligió una, guardamos un string vacío
                put(PerfilContract.Entry.COLUMN_IMAGE_PATH, "")
            }
        }

        // Lógica "UPSERT" (UPDATE o INSERT)
        // Reemplazará la fila si 'user_uid' ya existe,
        // o creará una fila nueva si no existe.
        val newRowId = db.insertWithOnConflict(
            PerfilContract.Entry.TABLE_NAME,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE // <-- Esto hace la magia
        )

        if (newRowId != -1L) {
            Toast.makeText(this, "Perfil actualizado con éxito", Toast.LENGTH_SHORT).show()
            cargarDatosDelMenu() // Actualiza la foto del menú lateral al instante
        } else {
            Toast.makeText(this, "Error al actualizar el perfil", Toast.LENGTH_SHORT).show()
        }
        // No cerramos la BD (db.close())
    }

    // Función de utilidad para obtener una ruta de archivo real desde una URI
    private fun getPathFromUri(context: Context, uri: Uri): String? {
        var path: String? = null
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(uri, projection, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val columnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                    path = it.getString(columnIndex)
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error al leer la ruta de la imagen", Toast.LENGTH_SHORT).show()
        }
        return path
    }
}