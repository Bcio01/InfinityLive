package com.saamael.infinitylive

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide // Librería para cargar imágenes fácilmente
import com.saamael.infinitylive.db.PerfilContract
import com.saamael.infinitylive.db.PerfilDbHelper
import com.saamael.infinitylive.databinding.ActivityPerfilBinding
import java.io.File

class PerfilActivity : BaseActivity() {

    private lateinit var binding: ActivityPerfilBinding
    private lateinit var dbHelper: PerfilDbHelper
    private var imagenUri: Uri? = null // Guardará la URI de la nueva imagen
    private var imagenPath: String? = null // Guardará la RUTA (String)

    // --- CRUD: READ ---
    // Lanzador para el resultado de "Elegir Foto"
    private val selectorDeImagen = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            imagenUri = uri
            binding.imgPerfil.setImageURI(uri) // Muestra la previsualización
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

        // Carga los datos
        cargarDatosDeFirebase()
        cargarDatosDeSQLite() // CRUD: READ

        // Configura los botones
        binding.btnCambiarFoto.setOnClickListener {
            selectorDeImagen.launch("image/*") // Abre la galería de fotos
        }

        binding.btnGuardarPerfil.setOnClickListener {
            guardarPerfilEnSQLite() // CRUD: UPDATE
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

    // --- CRUD: READ ---
    private fun cargarDatosDeSQLite() {
        val db = dbHelper.readableDatabase
        val cursor: Cursor = db.rawQuery(
            "SELECT * FROM ${PerfilContract.Entry.TABLE_NAME} WHERE ${PerfilContract.Entry.COLUMN_ID} = 1",
            null
        )

        if (cursor.moveToFirst()) {
            val biografia = cursor.getString(cursor.getColumnIndexOrThrow(PerfilContract.Entry.COLUMN_BIOGRAFIA))
            val pathFoto = cursor.getString(cursor.getColumnIndexOrThrow(PerfilContract.Entry.COLUMN_IMAGE_PATH))

            binding.etBiografia.setText(biografia)

            if (!pathFoto.isNullOrEmpty()) {
                // Carga la imagen desde el almacenamiento local usando Glide
                Glide.with(this)
                    .load(File(pathFoto)) // Carga desde la ruta del archivo
                    .circleCrop() // Opcional: la hace redonda
                    .into(binding.imgPerfil)
            }
        }
        cursor.close()
    }

    // --- CRUD: UPDATE ---
    private fun guardarPerfilEnSQLite() {
        val db = dbHelper.writableDatabase
        val biografia = binding.etBiografia.text.toString()

        // Usamos ContentValues, igual que en tu PDF [cite: 285]
        val values = ContentValues().apply {
            put(PerfilContract.Entry.COLUMN_BIOGRAFIA, biografia)
            // Solo actualiza la foto si el usuario seleccionó una nueva
            if (imagenPath != null) {
                put(PerfilContract.Entry.COLUMN_IMAGE_PATH, imagenPath)
            }
        }

        // Hacemos el UPDATE en la fila donde id = 1
        val selection = "${PerfilContract.Entry.COLUMN_ID} = ?"
        val selectionArgs = arrayOf("1")

        val count = db.update(
            PerfilContract.Entry.TABLE_NAME,
            values,
            selection,
            selectionArgs
        )


        if (count > 0) {
            Toast.makeText(this, "Perfil actualizado con éxito", Toast.LENGTH_SHORT).show()
            cargarDatosDelMenu()
        } else {
            Toast.makeText(this, "Error al actualizar el perfil", Toast.LENGTH_SHORT).show()
        }
    }

    // Función de utilidad para obtener una ruta de archivo real desde una URI
    private fun getPathFromUri(context: Context, uri: Uri): String? {
        var path: String? = null
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = context.contentResolver.query(uri, projection, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val columnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                path = it.getString(columnIndex)
            }
        }
        return path
    }
}