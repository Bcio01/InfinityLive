package com.saamael.infinitylive

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore
import com.saamael.infinitylive.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar Firebase Auth
        mAuth = FirebaseAuth.getInstance()

        // --- 1. LÓGICA DE LOGIN CON EMAIL/CONTRASEÑA (CORREGIDO) ---
        binding.btnAcceder.setOnClickListener {
            // Esto ahora llama a la función correcta
            iniciarSesionEmailPassword()
        }

        // --- 2. LÓGICA DE REGISTRO ---
        binding.tvRegistrate.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        // --- 3. LÓGICA DE LOGIN CON GOOGLE ---
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

        googleSignInLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)!!
                    firebaseAuthWithGoogle(account.idToken!!)
                } catch (e: ApiException) {
                    Toast.makeText(this, "Falló el inicio de sesión con Google", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Asignar clic al botón de Google
        binding.btnGoogleSignIn.setOnClickListener {
            // Llama a la función de Google
            signInGoogle()
        }
    }

    // --- FUNCIÓN PARA INICIAR SESIÓN CON EMAIL/PASS ---
// --- FUNCIÓN PARA INICIAR SESIÓN CON EMAIL/PASS (CORREGIDA Y SEGURA) ---
    private fun iniciarSesionEmailPassword() {
        val email = binding.etCorreo.text.toString().trim()
        val password = binding.etContrasena.text.toString().trim()

        // Limpiar errores anteriores
        binding.etCorreo.error = null
        binding.etContrasena.error = null

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Ingresa email y contraseña", Toast.LENGTH_SHORT).show()
            return
        }

        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = mAuth.currentUser
                    if (user != null) {
                        if (user.isEmailVerified) {
                            checkUserExists(user) // Usuario verificado, ¡adelante!
                        } else {
                            // Usuario no verificado
                            Toast.makeText(this, "Por favor, verifica tu correo electrónico para continuar.", Toast.LENGTH_LONG).show()
                            mAuth.signOut() // Desloguear
                        }
                    }
                } else {
                    // --- INICIO DE LA CORRECCIÓN DE SEGURIDAD ---
                    // Mostramos un mensaje genérico para cualquier error de credencial
                    try {
                        throw task.exception!!
                    }
                    catch (e: FirebaseAuthInvalidUserException) {
                        // Error: El correo no existe
                        mostrarErrorGenerico()
                    }
                    catch (e: FirebaseAuthInvalidCredentialsException) {
                        // Error: La contraseña es incorrecta
                        mostrarErrorGenerico()
                    }
                    catch (e: Exception) {
                        // Cualquier otro error
                        Toast.makeText(baseContext, "Error inesperado al iniciar sesión.", Toast.LENGTH_LONG).show()
                    }
                }
            }
    }

    /**
     * Muestra un error genérico para evitar la enumeración de usuarios.
     */
    private fun mostrarErrorGenerico() {
        binding.etCorreo.error = "Usuario o contraseña incorrectos"
        binding.etCorreo.requestFocus()
        Toast.makeText(baseContext, "Usuario o contraseña incorrectos.", Toast.LENGTH_LONG).show()
    }

    // --- FUNCIONES PARA INICIAR SESIÓN CON GOOGLE ---
    private fun signInGoogle() {
        val signInIntent = mGoogleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = mAuth.currentUser
                    if (user != null) {
                        checkUserExists(user) // Revisa si es usuario nuevo o antiguo
                    }
                } else {
                    Toast.makeText(this, "Falló la autenticación: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    // --- FUNCIÓN DE VERIFICACIÓN (NO CAMBIA) ---
    private fun checkUserExists(user: FirebaseUser) {
        val db = FirebaseFirestore.getInstance()
        val userDocRef = db.collection("users").document(user.uid)

        userDocRef.get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    goToInicioActivity() // Usuario antiguo
                } else {
                    goToSelectAreaActivity() // Usuario nuevo
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al verificar usuario: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun goToInicioActivity() {
        val intent = Intent(this, InicioActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finishAffinity()
    }

    private fun goToSelectAreaActivity() {
        val intent = Intent(this, SelectAreaActivity::class.java)
        startActivity(intent)
        finishAffinity()
    }
}