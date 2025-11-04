package com.saamael.infinitylive

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
// --- Importaciones de Errores ---
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
// ---
import com.saamael.infinitylive.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mAuth = FirebaseAuth.getInstance()

        binding.btnRegistrarme.setOnClickListener {
            registrarUsuario()
        }

        binding.tvIniciarSesion.setOnClickListener {
            finish()
        }
    }

    private fun registrarUsuario() {
        val nombre = binding.etNombre.text.toString().trim()
        val email = binding.etCorreo.text.toString().trim()
        val password = binding.etContrasena.text.toString().trim()
        val confirmPassword = binding.etConfirmarContrasena.text.toString().trim()

        // Limpiamos errores anteriores
        binding.etCorreo.error = null
        binding.etContrasena.error = null

        if (nombre.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }
        if (password != confirmPassword) {
            binding.etConfirmarContrasena.error = "Las contraseñas no coinciden"
            binding.etConfirmarContrasena.requestFocus()
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
            return
        }
        if (password.length < 6) {
            binding.etContrasena.error = "La contraseña debe tener al menos 6 caracteres"
            binding.etContrasena.requestFocus()
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
            return
        }

        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = mAuth.currentUser!!

                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(nombre)
                        .build()

                    user.updateProfile(profileUpdates)
                        .addOnCompleteListener { profileTask ->
                            if (profileTask.isSuccessful) {

                                user.sendEmailVerification()
                                    .addOnCompleteListener { verificationTask ->
                                        // No importa si el correo falla o no, el usuario debe avanzar
                                        if (verificationTask.isSuccessful) {
                                            Toast.makeText(this, "¡Registro exitoso! Revisa tu correo.", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(this, "Registro exitoso. Falló el envío de verificación.", Toast.LENGTH_SHORT).show()
                                        }

                                        // --- MUEVE LAS LÍNEAS AQUÍ ---
                                        // Navega a SelectArea SÓLO DESPUÉS de que el correo se envió
                                        val intent = Intent(this, SelectAreaActivity::class.java)
                                        startActivity(intent)
                                        finishAffinity()
                                        // ---
                                    }
                            }
                        }
                } else {
                    // --- MANEJO DE ERRORES DETALLADO ---
                    try {
                        throw task.exception!!
                    }

                    catch (e: FirebaseAuthWeakPasswordException) {
                        binding.etContrasena.error = "La contraseña es muy débil."
                        binding.etContrasena.requestFocus()
                        Toast.makeText(baseContext, "La contraseña es muy débil (mínimo 6 caracteres).", Toast.LENGTH_LONG).show()
                    }

                    catch (e: FirebaseAuthInvalidCredentialsException) {
                        binding.etCorreo.error = "El formato del correo es inválido."
                        binding.etCorreo.requestFocus()
                        Toast.makeText(baseContext, "El formato del correo es inválido.", Toast.LENGTH_LONG).show()
                    }

                    catch (e: FirebaseAuthUserCollisionException) {
                        binding.etCorreo.error = "Este correo ya está registrado."
                        binding.etCorreo.requestFocus()
                        Toast.makeText(baseContext, "Este correo ya está registrado.", Toast.LENGTH_LONG).show()
                    }

                    catch (e: Exception) {
                        Toast.makeText(baseContext, "Error inesperado: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                    // --- FIN DEL MANEJO DE ERRORES ---
                }
            }
    }
}