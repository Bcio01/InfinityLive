package com.saamael.infinitylive

import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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

    // Lanzador para el resultado de "Elegir Foto"
    private val selectorDeImagen = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            imagenUri = uri
            binding.imgPerfil.setImageURI(uri) // Muestra la previsualización
            // Convertimos la URI (temporal) a una ruta de archivo (permanente)
            imagenPath = getPathFromUri(this, uri)
        }
    }

// -----------------------------------------------------------------------------------
// FUNCIONES DE CICLO DE VIDA
// -----------------------------------------------------------------------------------

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

        // 📝 NUEVA LÓGICA: Editar Biografía
        binding.btnEditarBiografia.setOnClickListener {
            mostrarDialogoEdicionBiografia()
        }

        // 🗑️ NUEVA LÓGICA: Limpiar Perfil
        binding.btnEliminarPerfil.setOnClickListener {
            limpiarPerfilEnSQLite()
        }
    }

// -----------------------------------------------------------------------------------
// CARGA DE DATOS
// -----------------------------------------------------------------------------------

    private fun cargarDatosDeFirebase() {
        // Muestra los datos de Firebase (no editables)
        val user = mAuth.currentUser
        binding.tvPerfilNombre.text = "Nombre: ${user?.displayName ?: "No disponible"}"
        binding.tvPerfilEmail.text = "Email: ${user?.email ?: "No disponible"}"

        // El HP lo leemos en vivo
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

            // Muestra el hint si la biografía está vacía
            binding.tvBiografia.text = if (biografia.isEmpty()) binding.tvBiografia.hint else biografia

            if (!pathFoto.isNullOrEmpty()) {
                // Carga la imagen desde el almacenamiento local usando Glide
                Glide.with(this)
                    .load(File(pathFoto)) // Carga desde la ruta del archivo
                    .circleCrop() // Opcional: la hace redonda
                    .into(binding.imgPerfil)
            } else {
                // Si la ruta está vacía, carga la imagen por defecto
                binding.imgPerfil.setImageResource(R.drawable.usericon)
            }
        }
        cursor.close()
    }

// -----------------------------------------------------------------------------------
// FUNCIONALIDAD DE PERFIL
// -----------------------------------------------------------------------------------

    // --- CRUD: UPDATE ---
    private fun guardarPerfilEnSQLite() {
        val db = dbHelper.writableDatabase

        // Lee el texto del TextView, que fue actualizado por el diálogo
        val biografia = binding.tvBiografia.text.toString()

        val values = ContentValues().apply {
            // Solo guarda si no es el texto del hint
            put(PerfilContract.Entry.COLUMN_BIOGRAFIA, if (biografia == binding.tvBiografia.hint) "" else biografia)

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
            // Recarga los datos para refrescar la UI (por si la biografía era el hint)
            cargarDatosDeSQLite()
        } else {
            Toast.makeText(this, "Error al actualizar el perfil", Toast.LENGTH_SHORT).show()
        }
    }

    // 📝 IMPLEMENTACIÓN: Diálogo para editar la biografía
    private fun mostrarDialogoEdicionBiografia() {
        val input = EditText(this)
        input.hint = "Escribe tu nueva biografía..."

        // Establece el texto actual para la edición
        val currentBio = binding.tvBiografia.text.toString()
        if (currentBio != binding.tvBiografia.hint) {
            input.setText(currentBio)
        }

        AlertDialog.Builder(this)
            .setTitle("Editar Biografía")
            .setView(input)
            .setPositiveButton("Guardar") { dialog, _ ->
                val nuevaBiografia = input.text.toString().trim()

                // Actualiza el TextView con el nuevo texto (o el hint si está vacío)
                binding.tvBiografia.text = if (nuevaBiografia.isEmpty()) binding.tvBiografia.hint else nuevaBiografia

                Toast.makeText(this, "Biografía lista para guardar. Toca 'Guardar Cambios'.", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }

    // 🗑️ IMPLEMENTACIÓN: Limpiar Perfil
    private fun limpiarPerfilEnSQLite() {
        val db = dbHelper.writableDatabase

        // Valores para limpiar (vacío para biografía y ruta de imagen)
        val values = ContentValues().apply {
            put(PerfilContract.Entry.COLUMN_BIOGRAFIA, "")
            put(PerfilContract.Entry.COLUMN_IMAGE_PATH, "")
        }

        val selection = "${PerfilContract.Entry.COLUMN_ID} = ?"
        val selectionArgs = arrayOf("1")

        val count = db.update(
            PerfilContract.Entry.TABLE_NAME,
            values,
            selection,
            selectionArgs
        )

        if (count > 0) {
            Toast.makeText(this, "Perfil Local Limpiado. La biografía y la foto se han reseteado.", Toast.LENGTH_LONG).show()

            // --- Resetear UI y variables ---
            binding.tvBiografia.text = binding.tvBiografia.hint // Muestra el hint
            binding.imgPerfil.setImageResource(R.drawable.usericon) // Carga la imagen por defecto

            // Resetear las variables de la foto seleccionada
            imagenUri = null
            imagenPath = null

            cargarDatosDelMenu()
        } else {
            Toast.makeText(this, "Error al limpiar el perfil.", Toast.LENGTH_SHORT).show()
        }
    }

// -----------------------------------------------------------------------------------
// UTILIDAD
// -----------------------------------------------------------------------------------

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