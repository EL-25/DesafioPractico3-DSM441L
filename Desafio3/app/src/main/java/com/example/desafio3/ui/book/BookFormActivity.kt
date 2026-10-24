package com.example.desafio3.ui.book

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.example.desafio3.R
import com.example.desafio3.data.database.BookDatabaseHelper
import com.example.desafio3.data.model.Book
import com.example.desafio3.data.model.Genre
import com.example.desafio3.data.model.ReadingStatus
import com.example.desafio3.databinding.ActivityBookFormBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class BookFormActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_BOOK_ID = "extra_book_id"

        fun newIntent(context: Context, bookId: Long = -1L): Intent {
            return Intent(context, BookFormActivity::class.java).apply {
                putExtra(EXTRA_BOOK_ID, bookId)
            }
        }
    }

    private lateinit var binding: ActivityBookFormBinding
    private lateinit var dbHelper: BookDatabaseHelper
    private lateinit var auth: FirebaseAuth

    private var currentBookId: Long = -1L
    private var existingBook: Book? = null
    private var selectedRating: Int = 0
    private var starViews: List<ImageView> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = BookDatabaseHelper(this)
        auth = FirebaseAuth.getInstance()

        currentBookId = intent.getLongExtra(EXTRA_BOOK_ID, -1L)

        starViews = listOf(
            binding.ivStar1,
            binding.ivStar2,
            binding.ivStar3,
            binding.ivStar4,
            binding.ivStar5
        )

        setupToolbar()
        setupDropdowns()
        setupStarRating()
        setupTextWatchers()
        setupListeners()

        if (currentBookId > 0) {
            loadExistingBook()
        } else {
            // Default setup for new book
            binding.actvGenre.setText(Genre.NOVELA, false)
            binding.actvStatus.setText(ReadingStatus.POR_LEER, false)
            updateRatingStateForStatus(ReadingStatus.POR_LEER)
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        if (currentBookId > 0) {
            binding.toolbar.title = "Editar Libro"
            binding.btnDelete.visibility = View.VISIBLE
            binding.btnDelete.setOnClickListener {
                confirmDeleteBook()
            }
        } else {
            binding.toolbar.title = "Registrar Libro"
            binding.btnDelete.visibility = View.GONE
        }
    }

    private fun setupDropdowns() {
        // Dropdown de Géneros (exactamente los 4 géneros obligatorios)
        val genreAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, Genre.ALL)
        binding.actvGenre.setAdapter(genreAdapter)

        // Dropdown de Estados
        val statusAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, ReadingStatus.ALL)
        binding.actvStatus.setAdapter(statusAdapter)

        binding.actvStatus.setOnItemClickListener { _, _, position, _ ->
            val selectedStatus = ReadingStatus.ALL[position]
            updateRatingStateForStatus(selectedStatus)

            // Si pasa a terminado y las páginas leídas están vacías o incompletas, auto-sugerir totalPages
            if (selectedStatus == ReadingStatus.TERMINADO) {
                val totalStr = binding.etTotalPages.text?.toString()?.trim().orEmpty()
                if (totalStr.isNotEmpty()) {
                    binding.etReadPages.setText(totalStr)
                }
            }
        }
    }

    private fun updateRatingStateForStatus(status: String) {
        val isFinished = status == ReadingStatus.TERMINADO
        if (isFinished) {
            // Habilitar calificación obligatoria
            binding.cardRatingSection.alpha = 1.0f
            binding.tvRatingLabel.text = "Calificación del libro *"
            binding.tvRatingHint.text = "Obligatoria (asigna de 1 a 5 estrellas al terminar)."
            binding.tvRatingHint.setTextColor(getColor(R.color.rating_star))
            binding.tvRatingError.visibility = View.GONE
            setStarsClickable(true)
            binding.tvFinishDateInfo.visibility = View.VISIBLE
            val dateText = existingBook?.finishDate ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            binding.tvFinishDateInfo.text = "Fecha de finalización: $dateText"
        } else {
            // Deshabilitar y limpiar calificación
            binding.cardRatingSection.alpha = 0.5f
            binding.tvRatingLabel.text = "Calificación del libro (Deshabilitada)"
            binding.tvRatingHint.text = "Solo se puede calificar cuando el estado sea 'Terminado'."
            binding.tvRatingHint.setTextColor(getColor(R.color.text_secondary))
            binding.tvRatingError.visibility = View.GONE
            selectedRating = 0
            updateStarUi(0)
            setStarsClickable(false)
            binding.tvFinishDateInfo.visibility = View.GONE
        }
    }

    private fun setupStarRating() {
        starViews.forEachIndexed { index, imageView ->
            imageView.setOnClickListener {
                val currentStatus = binding.actvStatus.text.toString().trim()
                if (currentStatus == ReadingStatus.TERMINADO) {
                    selectedRating = index + 1
                    updateStarUi(selectedRating)
                    binding.tvRatingError.visibility = View.GONE
                } else {
                    Toast.makeText(
                        this,
                        "Solo puedes calificar un libro si su estado es 'Terminado'.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun setStarsClickable(clickable: Boolean) {
        starViews.forEach { it.isEnabled = clickable }
    }

    private fun updateStarUi(rating: Int) {
        starViews.forEachIndexed { index, imageView ->
            if (index < rating) {
                imageView.setImageResource(R.drawable.ic_star_filled)
            } else {
                imageView.setImageResource(R.drawable.ic_star_empty)
            }
        }
        binding.tvRatingValue.text = if (rating > 0) "$rating / 5 estrellas" else "Sin calificar"
    }

    private fun setupTextWatchers() {
        binding.etTitle.doAfterTextChanged { binding.tilTitle.error = null }
        binding.etAuthor.doAfterTextChanged { binding.tilAuthor.error = null }
        binding.actvGenre.doAfterTextChanged { binding.tilGenre.error = null }
        binding.etTotalPages.doAfterTextChanged { binding.tilTotalPages.error = null }
        binding.etReadPages.doAfterTextChanged { binding.tilReadPages.error = null }
        binding.actvStatus.doAfterTextChanged { binding.tilStatus.error = null }
    }

    private fun setupListeners() {
        binding.btnSaveBook.setOnClickListener {
            saveBook()
        }
    }

    private fun loadExistingBook() {
        val userId = auth.currentUser?.uid ?: return
        val book = dbHelper.getBookById(currentBookId, userId)
        if (book != null) {
            existingBook = book
            binding.etTitle.setText(book.title)
            binding.etAuthor.setText(book.author)
            binding.actvGenre.setText(book.genre, false)
            binding.etTotalPages.setText(book.totalPages.toString())
            binding.etReadPages.setText(book.readPages.toString())
            binding.actvStatus.setText(book.status, false)

            selectedRating = book.rating
            updateRatingStateForStatus(book.status)
            updateStarUi(book.rating)

            if (book.finishDate != null) {
                binding.tvFinishDateInfo.visibility = View.VISIBLE
                binding.tvFinishDateInfo.text = "Fecha de finalización: ${book.finishDate}"
            }
        } else {
            Toast.makeText(this, "No se encontró el libro", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun saveBook() {
        val title = binding.etTitle.text?.toString()?.trim().orEmpty()
        val author = binding.etAuthor.text?.toString()?.trim().orEmpty()
        val genre = binding.actvGenre.text?.toString()?.trim().orEmpty()
        val totalPagesStr = binding.etTotalPages.text?.toString()?.trim().orEmpty()
        val readPagesStr = binding.etReadPages.text?.toString()?.trim().orEmpty()
        val status = binding.actvStatus.text?.toString()?.trim().orEmpty()

        val errors = mutableListOf<String>()

        // 1. Validación de Título: obligatorio y entre 2 y 60 caracteres
        if (title.isEmpty()) {
            binding.tilTitle.error = "El título es obligatorio"
            errors.add("Título: es obligatorio (debe tener entre 2 y 60 caracteres).")
        } else if (title.length < 2 || title.length > 60) {
            binding.tilTitle.error = "Debe tener entre 2 y 60 caracteres (actual: ${title.length})"
            errors.add("Título: longitud inválida. Debe tener entre 2 y 60 caracteres.")
        } else {
            binding.tilTitle.error = null
        }

        // 2. Validación de Autor: obligatorio y entre 2 y 60 caracteres
        if (author.isEmpty()) {
            binding.tilAuthor.error = "El autor es obligatorio"
            errors.add("Autor: es obligatorio (debe tener entre 2 y 60 caracteres).")
        } else if (author.length < 2 || author.length > 60) {
            binding.tilAuthor.error = "Debe tener entre 2 y 60 caracteres (actual: ${author.length})"
            errors.add("Autor: longitud inválida. Debe tener entre 2 y 60 caracteres.")
        } else {
            binding.tilAuthor.error = null
        }

        // 3. Validación de Género: obligatorio y uno de los 4 permitidos
        if (genre.isEmpty() || !Genre.ALL.contains(genre)) {
            binding.tilGenre.error = "Debes seleccionar uno de los cuatro géneros obligatorios"
            errors.add("Género: inválido. Debe ser: Novela, Ciencia ficción, Historia o Desarrollo personal.")
        } else {
            binding.tilGenre.error = null
        }

        // 4. Validación de Total de Páginas: entero > 0
        var totalPages = 0
        if (totalPagesStr.isEmpty()) {
            binding.tilTotalPages.error = "Obligatorio"
            errors.add("Total de páginas: es obligatorio y debe ser un número entero mayor que 0.")
        } else {
            try {
                totalPages = totalPagesStr.toInt()
                if (totalPages <= 0) {
                    binding.tilTotalPages.error = "Debe ser mayor a 0"
                    errors.add("Total de páginas: debe ser un número entero mayor que 0.")
                } else {
                    binding.tilTotalPages.error = null
                }
            } catch (e: NumberFormatException) {
                binding.tilTotalPages.error = "Número no válido"
                errors.add("Total de páginas: formato numérico inválido.")
            }
        }

        // 5. Validación de Páginas Leídas: entero entre 0 y totalPages
        var readPages = 0
        if (readPagesStr.isEmpty()) {
            binding.tilReadPages.error = "Obligatorio"
            errors.add("Páginas leídas: es obligatorio (ingresa 0 si aún no has comenzado).")
        } else {
            try {
                readPages = readPagesStr.toInt()
                if (readPages < 0) {
                    binding.tilReadPages.error = "No puede ser menor a 0"
                    errors.add("Páginas leídas: no puede ser un número negativo.")
                } else if (totalPages > 0 && readPages > totalPages) {
                    binding.tilReadPages.error = "No puede superar el total ($totalPages)"
                    errors.add("Páginas leídas: no puede superar el total de páginas ($totalPages).")
                } else {
                    binding.tilReadPages.error = null
                }
            } catch (e: NumberFormatException) {
                binding.tilReadPages.error = "Número no válido"
                errors.add("Páginas leídas: formato numérico inválido.")
            }
        }

        // 6. Validación de Estado: Por leer, Leyendo, Terminado
        if (status.isEmpty() || !ReadingStatus.ALL.contains(status)) {
            binding.tilStatus.error = "Selecciona un estado de lectura válido"
            errors.add("Estado: debe ser 'Por leer', 'Leyendo' o 'Terminado'.")
        } else {
            binding.tilStatus.error = null
        }

        // 7. Validación de Calificación (1 a 5 estrellas):
        // Solo puede asignarse si el estado es Terminado, y en ese caso es obligatoria
        val isFinished = status == ReadingStatus.TERMINADO
        if (isFinished) {
            if (selectedRating < 1 || selectedRating > 5) {
                binding.tvRatingError.visibility = View.VISIBLE
                binding.tvRatingError.text = "La calificación es obligatoria para libros terminados (1 a 5 estrellas)"
                errors.add("Calificación: obligatoria (1 a 5 estrellas) para libros en estado Terminado.")
            } else {
                binding.tvRatingError.visibility = View.GONE
            }

            // Si está terminado, las páginas leídas deben ser iguales al total de páginas
            if (totalPages > 0 && readPages < totalPages) {
                // Ajustar automáticamente para consistencia
                readPages = totalPages
                binding.etReadPages.setText(totalPages.toString())
            }
        } else {
            // Si el estado no es terminado, la calificación no puede asignarse (se fuerza a 0)
            selectedRating = 0
            binding.tvRatingError.visibility = View.GONE
        }

        // Si existe algún error, mostrar cuadro con qué campo y por qué
        if (errors.isNotEmpty()) {
            val message = errors.joinToString(separator = "\n• ", prefix = "Por favor corrige los siguientes campos:\n• ")
            AlertDialog.Builder(this)
                .setTitle("Datos incompletos o inválidos")
                .setMessage(message)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Entendido", null)
                .show()
            return
        }

        // Fecha de finalización
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val nowTimestamp = System.currentTimeMillis()
        val formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val finishDate: String?
        val finishYear: Int?
        val finishTimestamp: Long?

        if (isFinished) {
            // Si ya tenía fecha previa, conservarla, si no, asignar la actual
            finishDate = existingBook?.finishDate ?: formattedDate
            finishYear = existingBook?.finishYear ?: currentYear
            finishTimestamp = existingBook?.finishTimestamp ?: nowTimestamp
        } else {
            finishDate = null
            finishYear = null
            finishTimestamp = null
        }

        val userId = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "Error de sesión de usuario", Toast.LENGTH_SHORT).show()
            return
        }

        val bookToSave = Book(
            id = if (currentBookId > 0) currentBookId else 0,
            userId = userId,
            title = title,
            author = author,
            genre = genre,
            totalPages = totalPages,
            readPages = readPages,
            status = status,
            rating = selectedRating,
            finishDate = finishDate,
            finishYear = finishYear,
            finishTimestamp = finishTimestamp,
            createdAt = existingBook?.createdAt ?: nowTimestamp
        )

        if (currentBookId > 0) {
            val updated = dbHelper.updateBook(bookToSave)
            if (updated > 0) {
                Toast.makeText(this, "Libro actualizado correctamente", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Error al actualizar el libro", Toast.LENGTH_SHORT).show()
            }
        } else {
            val id = dbHelper.insertBook(bookToSave)
            if (id != -1L) {
                Toast.makeText(this, "Libro registrado exitosamente", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Error al guardar el libro", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirmDeleteBook() {
        AlertDialog.Builder(this)
            .setTitle("Eliminar libro")
            .setMessage("¿Estás seguro de que deseas eliminar este libro? Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                val userId = auth.currentUser?.uid ?: return@setPositiveButton
                val deleted = dbHelper.deleteBook(currentBookId, userId)
                if (deleted > 0) {
                    Toast.makeText(this, "Libro eliminado", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Error al eliminar el libro", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
