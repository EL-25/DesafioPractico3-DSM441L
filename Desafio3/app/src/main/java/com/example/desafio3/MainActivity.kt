package com.example.desafio3

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.desafio3.data.database.BookDatabaseHelper
import com.example.desafio3.databinding.ActivityMainBinding
import com.example.desafio3.databinding.DialogEditGoalBinding
import com.example.desafio3.ui.adapter.BookAdapter
import com.example.desafio3.ui.auth.AuthActivity
import com.example.desafio3.ui.book.BookFormActivity
import com.example.desafio3.ui.library.LibraryActivity
import com.example.desafio3.ui.stats.StatsActivity
import com.google.firebase.auth.FirebaseAuth
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var dbHelper: BookDatabaseHelper
    private lateinit var readingAdapter: BookAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        // Verificación de sesión: si no está autenticado, redirigir al login
        val currentUser = auth.currentUser
        if (currentUser == null) {
            goToAuthActivity()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = BookDatabaseHelper(this)

        setupToolbar()
        setupGreeting()
        setupRecyclerView()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        if (auth.currentUser != null) {
            loadDashboardData()
        }
    }

    private fun setupToolbar() {
        binding.btnLogout.setOnClickListener {
            confirmLogout()
        }
    }

    private fun setupGreeting() {
        val user = auth.currentUser
        val email = user?.email.orEmpty()
        val nameFromEmail = email.substringBefore("@").replaceFirstChar { it.uppercase() }
        val displayName = user?.displayName?.ifEmpty { null } ?: nameFromEmail

        binding.tvGreeting.text = "¡Hola, $displayName!"
        binding.tvUserEmail.text = email
    }

    private fun setupRecyclerView() {
        readingAdapter = BookAdapter { book ->
            val intent = BookFormActivity.newIntent(this, book.id)
            startActivity(intent)
        }
        binding.rvCurrentlyReading.layoutManager = LinearLayoutManager(this)
        binding.rvCurrentlyReading.adapter = readingAdapter
    }

    private fun setupListeners() {
        // Modificar Meta Anual
        binding.btnEditGoal.setOnClickListener {
            showEditGoalDialog()
        }

        // Acceso Rápido: Agregar Libro
        binding.btnQuickAddBook.setOnClickListener {
            startActivity(BookFormActivity.newIntent(this))
        }

        // FAB: Agregar Libro
        binding.fabAdd.setOnClickListener {
            startActivity(BookFormActivity.newIntent(this))
        }

        // Acceso Rápido: Mi Biblioteca
        binding.btnQuickLibrary.setOnClickListener {
            startActivity(Intent(this, LibraryActivity::class.java))
        }

        // Acceso Rápido: Estadísticas
        binding.btnQuickStats.setOnClickListener {
            startActivity(Intent(this, StatsActivity::class.java))
        }
    }

    private fun loadDashboardData() {
        val userId = auth.currentUser?.uid ?: return
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        // 1. Datos de la Meta Anual
        binding.tvGoalTitle.text = "Meta de Lectura $currentYear"
        val annualGoal = dbHelper.getAnnualGoal(userId, currentYear)
        val finishedCount = dbHelper.getFinishedBooksCountInYear(userId, currentYear)
        val remaining = (annualGoal - finishedCount).coerceAtLeast(0)

        binding.tvGoalTarget.text = annualGoal.toString()
        binding.tvGoalFinished.text = finishedCount.toString()
        binding.tvGoalRemaining.text = remaining.toString()

        val goalPercentage = if (annualGoal > 0) {
            ((finishedCount.toDouble() / annualGoal.toDouble()) * 100).toInt().coerceIn(0, 100)
        } else {
            0
        }
        binding.progressGoal.progress = goalPercentage

        if (finishedCount >= annualGoal && annualGoal > 0) {
            binding.tvGoalStatusMessage.text = "🎉 ¡Felicidades! Has cumplido tu meta anual ($goalPercentage%)"
            binding.tvGoalStatusMessage.setTextColor(getColor(R.color.secondary))
        } else {
            binding.tvGoalStatusMessage.text = "$goalPercentage% completado • Faltan $remaining ${if (remaining == 1) "libro" else "libros"}"
            binding.tvGoalStatusMessage.setTextColor(getColor(R.color.text_secondary))
        }

        // 2. Libros leyendo actualmente
        val currentlyReading = dbHelper.getCurrentlyReadingBooks(userId)
        binding.tvReadingCount.text = "${currentlyReading.size} ${if (currentlyReading.size == 1) "libro" else "libros"}"

        if (currentlyReading.isEmpty()) {
            binding.rvCurrentlyReading.visibility = View.GONE
            binding.cardEmptyReading.visibility = View.VISIBLE
        } else {
            binding.rvCurrentlyReading.visibility = View.VISIBLE
            binding.cardEmptyReading.visibility = View.GONE
            readingAdapter.submitList(currentlyReading)
        }
    }

    private fun showEditGoalDialog() {
        val userId = auth.currentUser?.uid ?: return
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val currentGoal = dbHelper.getAnnualGoal(userId, currentYear)

        val dialogBinding = DialogEditGoalBinding.inflate(LayoutInflater.from(this))
        dialogBinding.tvDialogTitle.text = "Meta Anual de Lectura $currentYear"
        dialogBinding.etAnnualGoal.setText(currentGoal.toString())
        dialogBinding.etAnnualGoal.selectAll()

        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setPositiveButton("Guardar", null) // Lo configuramos abajo para validar sin cerrar prematuramente
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.setOnShowListener {
            val saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            saveButton.setOnClickListener {
                val inputStr = dialogBinding.etAnnualGoal.text?.toString()?.trim().orEmpty()
                if (inputStr.isEmpty()) {
                    dialogBinding.tilAnnualGoal.error = "Por favor ingresa una cantidad"
                    return@setOnClickListener
                }

                try {
                    val newGoal = inputStr.toInt()
                    if (newGoal <= 0) {
                        dialogBinding.tilAnnualGoal.error = "La meta debe ser un número entero mayor que cero"
                        return@setOnClickListener
                    }

                    // Guardar en la base de datos
                    val success = dbHelper.setAnnualGoal(userId, currentYear, newGoal)
                    if (success) {
                        Toast.makeText(this, "Meta anual actualizada a $newGoal libros", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        loadDashboardData()
                    } else {
                        Toast.makeText(this, "Error al guardar la meta", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: NumberFormatException) {
                    dialogBinding.tilAnnualGoal.error = "Ingresa un número entero válido"
                }
            }
        }

        dialog.show()
    }

    private fun confirmLogout() {
        AlertDialog.Builder(this)
            .setTitle("Cerrar Sesión")
            .setMessage("¿Estás seguro de que deseas cerrar sesión?")
            .setPositiveButton("Cerrar Sesión") { _, _ ->
                auth.signOut()
                Toast.makeText(this, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show()
                goToAuthActivity()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun goToAuthActivity() {
        val intent = Intent(this, AuthActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}