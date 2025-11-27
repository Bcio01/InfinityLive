package com.saamael.infinitylive

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.Glide
import com.saamael.infinitylive.db.PerfilContract
import com.saamael.infinitylive.db.PerfilDbHelper
import com.saamael.infinitylive.databinding.ActivityPerfilBinding
import java.io.File

class PerfilActivity : BaseActivity() {

    private lateinit var binding: ActivityPerfilBinding
    private lateinit var dbHelper: PerfilDbHelper

    // Almacenamiento temporal de los cambios
    private var biografiaTemporal: String? = null
    private var imagenPath: String? = null // Guardará la RUTA (String) actual o nueva

    // Lanzador para el resultado de "Elegir Foto"
    private val selectorDeImagen = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            // Carga la previsualización
            Glide.with(this)
                .load(uri)
                .circleCrop()
                .into(binding.imgPerfil)

            // Guarda la ruta para cuando el usuario presione "Guardar"
            imagenPath = getPathFromUri(this, uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPerfilBinding.inflate(layoutInflater)
        baseBinding.contentFrame.addView(binding.root)

        dbHelper = PerfilDbHelper(this)

        setupBottomMenu()
        logout()

        if (uid != null) {
            cargarDatosDeFirebase()
            cargarDatosDeSQLite() // CRUD: READ
        } else {
            // Si no hay usuario, regresa al Login
            Toast.makeText(this, "Error de sesión", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }



        // --- Configuración de Botones ---

        binding.btnCambiarFoto.setOnClickListener {
            selectorDeImagen.launch("image/*") // Abre la galería
        }

        binding.btnEditarBiografia.setOnClickListener {
            abrirDialogoBiografia()
        }

        binding.btnGuardarPerfil.setOnClickListener {
            guardarPerfilEnSQLite() // CRUD: UPDATE/CREATE
        }

        binding.btnEliminarPerfil.setOnClickListener {
            confirmarEliminacion() // CRUD: DELETE
        }
    }

    private fun cargarDatosDeFirebase() {
        // ... (esta función no cambia, está bien)
        val user = mAuth.currentUser
        binding.tvPerfilNombre.text = "Nombre: ${user?.displayName ?: "No disponible"}"
        binding.tvPerfilEmail.text = "Email: ${user?.email ?: "No disponible"}"
        db.collection("users").document(uid!!).get().addOnSuccessListener { doc ->
            val hp = doc.getLong("avatar_hp") ?: 1000L
            binding.tvPerfilHp.text = "HP: $hp / 1000"
        }
    }

    // --- CRUD: READ (CORREGIDO) ---
    private fun cargarDatosDeSQLite() {
        if (uid == null) return

        val db = dbHelper.readableDatabase
        val cursor: Cursor = db.rawQuery(
            // --- ARREGLO AQUÍ ---
            // Busca la fila que coincida con el UID del usuario actual
            "SELECT * FROM ${PerfilContract.Entry.TABLE_NAME} WHERE ${PerfilContract.Entry.COLUMN_USER_UID} = ?",
            arrayOf(uid) // <-- USA EL UID REAL, NO "1"
        )

        if (cursor.moveToFirst()) {
            val biografia = cursor.getString(cursor.getColumnIndexOrThrow(PerfilContract.Entry.COLUMN_BIOGRAFIA))
            val pathFoto = cursor.getString(cursor.getColumnIndexOrThrow(PerfilContract.Entry.COLUMN_IMAGE_PATH))

            // 1. Carga la Biografía en el TextView
            if (biografia.isNullOrEmpty()) {
                binding.tvBiografia.text = "Toca el lápiz para añadir una biografía..."
            } else {
                binding.tvBiografia.text = biografia
            }
            biografiaTemporal = biografia // Carga en la variable temporal

            // 2. Carga la Foto
            if (!pathFoto.isNullOrEmpty()) {
                imagenPath = pathFoto // Guarda la ruta actual
                Glide.with(this)
                    .load(File(pathFoto))
                    .circleCrop()
                    .into(binding.imgPerfil)
            } else {
                imagenPath = null
                binding.imgPerfil.setImageResource(R.drawable.usericon)
            }
        }
        cursor.close()
    }

    // --- Lógica del nuevo flujo de Biografía ---
    private fun abrirDialogoBiografia() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Actualizar Biografía")

        // Inflar un layout simple con un EditText
        val inflater = layoutInflater
        // Asumiendo que 'dialog_editar_biografia.xml' existe en res/layout
        val dialogView = inflater.inflate(R.layout.dialog_editar_biografia, null)
        val etBioDialog = dialogView.findViewById<EditText>(R.id.etBioDialog)

        // Pone el texto actual en el diálogo
        etBioDialog.setText(biografiaTemporal ?: "")

        builder.setView(dialogView)

        builder.setPositiveButton("Guardar") { dialog, _ ->
            // SOLO guarda el texto en la variable temporal
            biografiaTemporal = etBioDialog.text.toString()
            // Actualiza la UI al instante
            binding.tvBiografia.text = if (biografiaTemporal.isNullOrEmpty()) "Toca el lápiz para añadir una biografía..." else biografiaTemporal
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    // --- CRUD: UPDATE / CREATE (CORREGIDO) ---
    private fun guardarPerfilEnSQLite() {
        if (uid == null) return

        val db = dbHelper.writableDatabase

        val values = ContentValues().apply {
            put(PerfilContract.Entry.COLUMN_USER_UID, uid) // <-- ARREGLO AQUÍ: Usa el UID real
            // Guarda la biografía desde la variable temporal
            put(PerfilContract.Entry.COLUMN_BIOGRAFIA, biografiaTemporal ?: "")
            // Guarda la foto desde la variable temporal
            put(PerfilContract.Entry.COLUMN_IMAGE_PATH, imagenPath ?: "")
        }

        // Lógica "UPSERT" (UPDATE o INSERT)
        // Esto reemplazará la fila si 'user_uid' ya existe,
        // o creará una fila nueva si no existe.
        val newRowId = db.insertWithOnConflict(
            PerfilContract.Entry.TABLE_NAME,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE // <-- Esto hace la magia
        )

        if (newRowId != -1L) {
            Toast.makeText(this, "Perfil actualizado con éxito", Toast.LENGTH_SHORT).show()
            cargarDatosDelMenu() // Actualiza el menú lateral
        } else {
            Toast.makeText(this, "Error al actualizar el perfil", Toast.LENGTH_SHORT).show()
        }
    }

    // --- CRUD: DELETE (CORREGIDO) ---
    private fun confirmarEliminacion() {
        AlertDialog.Builder(this)
            .setTitle("Limpiar Perfil")
            .setMessage("¿Estás seguro de que quieres eliminar tu foto y biografía?")
            .setPositiveButton("Sí, Limpiar") { dialog, _ ->
                eliminarDatosDeSQLite()
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun eliminarDatosDeSQLite() {
        if (uid == null) return
        val db = dbHelper.writableDatabase

        // No borramos la fila, solo limpiamos los campos
        val values = ContentValues().apply {
            put(PerfilContract.Entry.COLUMN_IMAGE_PATH, "")
            put(PerfilContract.Entry.COLUMN_BIOGRAFIA, "")
        }

        val selection = "${PerfilContract.Entry.COLUMN_USER_UID} = ?"
        val selectionArgs = arrayOf(uid) // <-- ARREGLO AQUÍ: Usa el UID real, NO "1"

        db.update(
            PerfilContract.Entry.TABLE_NAME,
            values,
            selection,
            selectionArgs
        )

        Toast.makeText(this, "Perfil limpiado", Toast.LENGTH_SHORT).show()
        // Recarga la UI para mostrar los campos vacíos
        cargarDatosDeSQLite()
        cargarDatosDelMenu()
    }

    // --- (Función getPathFromUri no cambia, está bien) ---
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

    private fun logout() {
        binding.logoutMenu.setOnClickListener {
            mAuth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    // --- NAVEGACIÓN MENU INFERIOR ---
    private fun setupBottomMenu() {
        // 1. Botón Inicio (Volver a InicioActivity)
        binding.MenuInferior.menuHabitos.setOnClickListener {
            val intent = Intent(this, InicioActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(intent)
            // finish() // Opcional: Si quieres cerrar Perfil al salir
        }

        // 2. Botón Rutinas
        binding.MenuInferior.menuDiarias.setOnClickListener {
            val intent = Intent(this, RutinasActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(intent)
        }

        // 3. Botón Central (Crear)
        binding.MenuInferior.fabDiamondContainer.setOnClickListener {
            val intent = Intent(this, GestionHabitosActivity::class.java)
            startActivity(intent)
        }

        // 5. Tienda
        binding.MenuInferior.menuRecompensas.setOnClickListener {
            val intent = Intent(this, TiendaActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(intent)
        }
    }

}