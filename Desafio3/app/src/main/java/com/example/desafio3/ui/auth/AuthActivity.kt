package com.example.desafio3.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.example.desafio3.MainActivity
import com.example.desafio3.R
import com.example.desafio3.databinding.ActivityAuthBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private lateinit var auth: FirebaseAuth
    private var isLoginMode = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Check if already authenticated (Session persistence)
        if (auth.currentUser != null) {
            goToMainActivity()
            return
        }

        setupToggleGroup()
        setupListeners()
        setupTextWatchers()
    }

    private fun setupToggleGroup() {
        binding.toggleAuthMode.check(R.id.btnTabLogin)
        updateUiMode(true)

        binding.toggleAuthMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                updateUiMode(checkedId == R.id.btnTabLogin)
            }
        }
    }

    private fun updateUiMode(isLogin: Boolean) {
        isLoginMode = isLogin
        clearErrors()

        if (isLogin) {
            binding.tvFormTitle.text = "Iniciar Sesión"
            binding.btnSubmit.text = "Iniciar Sesión"
            binding.tilConfirmPassword.visibility = View.GONE
            binding.tvSwitchPrompt.text = "¿No tienes una cuenta? Regístrate aquí"
        } else {
            binding.tvFormTitle.text = "Crear Cuenta Nueva"
            binding.btnSubmit.text = "Registrarme"
            binding.tilConfirmPassword.visibility = View.VISIBLE
            binding.tvSwitchPrompt.text = "¿Ya tienes cuenta? Inicia sesión aquí"
        }
    }

    private fun setupListeners() {
        binding.btnSubmit.setOnClickListener {
            if (validateInputs()) {
                if (isLoginMode) {
                    performLogin()
                } else {
                    performRegister()
                }
            }
        }

        binding.tvSwitchPrompt.setOnClickListener {
            if (isLoginMode) {
                binding.toggleAuthMode.check(R.id.btnTabRegister)
            } else {
                binding.toggleAuthMode.check(R.id.btnTabLogin)
            }
        }
    }

    private fun setupTextWatchers() {
        binding.etEmail.doAfterTextChanged { binding.tilEmail.error = null }
        binding.etPassword.doAfterTextChanged { binding.tilPassword.error = null }
        binding.etConfirmPassword.doAfterTextChanged { binding.tilConfirmPassword.error = null }
    }

    private fun clearErrors() {
        binding.tilEmail.error = null
        binding.tilPassword.error = null
        binding.tilConfirmPassword.error = null
    }

    private fun validateInputs(): Boolean {
        var isValid = true
        val email = binding.etEmail.text?.toString()?.trim().orEmpty()
        val password = binding.etPassword.text?.toString()?.trim().orEmpty()
        val confirmPassword = binding.etConfirmPassword.text?.toString()?.trim().orEmpty()

        // Validar correo
        if (email.isEmpty()) {
            binding.tilEmail.error = "El correo electrónico es obligatorio"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Formato de correo no válido (ejemplo: usuario@correo.com)"
            isValid = false
        } else {
            binding.tilEmail.error = null
        }

        // Validar contraseña
        if (password.isEmpty()) {
            binding.tilPassword.error = "La contraseña es obligatoria"
            isValid = false
        } else if (password.length < 6) {
            binding.tilPassword.error = "La contraseña debe tener al menos 6 caracteres"
            isValid = false
        } else {
            binding.tilPassword.error = null
        }

        // Validar confirmación de contraseña en registro
        if (!isLoginMode) {
            if (confirmPassword.isEmpty()) {
                binding.tilConfirmPassword.error = "Confirma tu contraseña"
                isValid = false
            } else if (password != confirmPassword) {
                binding.tilConfirmPassword.error = "Las contraseñas no coinciden"
                isValid = false
            } else {
                binding.tilConfirmPassword.error = null
            }
        }

        return isValid
    }

    private fun performLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        setLoading(true)
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                setLoading(false)
                if (task.isSuccessful) {
                    Toast.makeText(this, "¡Bienvenido de vuelta!", Toast.LENGTH_SHORT).show()
                    goToMainActivity()
                }
            }
            .addOnFailureListener { exception ->
                setLoading(false)
                val errorMessage = when (exception) {
                    is FirebaseAuthInvalidUserException -> "No existe una cuenta registrada con este correo"
                    is FirebaseAuthInvalidCredentialsException -> "Credenciales inválidas o contraseña incorrecta"
                    is FirebaseNetworkException -> "Error de conexión a internet. Verifica tu red"
                    else -> exception.localizedMessage ?: "Error al iniciar sesión"
                }
                showError(errorMessage)
            }
    }

    private fun performRegister() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        setLoading(true)
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                setLoading(false)
                if (task.isSuccessful) {
                    Toast.makeText(this, "¡Cuenta creada exitosamente! Bienvenido", Toast.LENGTH_SHORT).show()
                    goToMainActivity()
                }
            }
            .addOnFailureListener { exception ->
                setLoading(false)
                val errorMessage = when (exception) {
                    is FirebaseAuthUserCollisionException -> "Ya existe una cuenta con este correo electrónico"
                    is FirebaseAuthWeakPasswordException -> "La contraseña es demasiado débil (mínimo 6 caracteres)"
                    is FirebaseAuthInvalidCredentialsException -> "El formato del correo es inválido"
                    is FirebaseNetworkException -> "Error de conexión a internet. Verifica tu red"
                    else -> exception.localizedMessage ?: "Error al registrar usuario"
                }
                showError(errorMessage)
            }
    }

    private fun setLoading(loading: Boolean) {
        binding.pbLoading.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnSubmit.isEnabled = !loading
        binding.etEmail.isEnabled = !loading
        binding.etPassword.isEnabled = !loading
        binding.etConfirmPassword.isEnabled = !loading
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(getColor(R.color.error_color))
            .setTextColor(getColor(R.color.white))
            .show()
    }

    private fun goToMainActivity() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
