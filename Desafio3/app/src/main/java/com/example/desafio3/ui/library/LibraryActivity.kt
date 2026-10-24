package com.example.desafio3.ui.library

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.desafio3.R
import com.example.desafio3.data.database.BookDatabaseHelper
import com.example.desafio3.data.model.Book
import com.example.desafio3.data.model.ReadingStatus
import com.example.desafio3.databinding.ActivityLibraryBinding
import com.example.desafio3.ui.adapter.BookAdapter
import com.example.desafio3.ui.book.BookFormActivity
import com.google.firebase.auth.FirebaseAuth

class LibraryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLibraryBinding
    private lateinit var dbHelper: BookDatabaseHelper
    private lateinit var auth: FirebaseAuth
    private lateinit var adapter: BookAdapter

    private var allBooks: List<Book> = emptyList()
    private var currentFilterStatus: String? = null // null means "Todos"
    private var currentSearchQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLibraryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = BookDatabaseHelper(this)
        auth = FirebaseAuth.getInstance()

        setupToolbar()
        setupRecyclerView()
        setupFilterChips()
        setupSearch()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        loadBooks()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = BookAdapter { book ->
            val intent = BookFormActivity.newIntent(this, book.id)
            startActivity(intent)
        }
        binding.rvBooks.layoutManager = LinearLayoutManager(this)
        binding.rvBooks.adapter = adapter
    }

    private fun setupFilterChips() {
        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            currentFilterStatus = when {
                checkedIds.contains(R.id.chipFilterLeyendo) -> ReadingStatus.LEYENDO
                checkedIds.contains(R.id.chipFilterTerminado) -> ReadingStatus.TERMINADO
                checkedIds.contains(R.id.chipFilterPorLeer) -> ReadingStatus.POR_LEER
                else -> null
            }
            applyFilters()
        }
    }

    private fun setupSearch() {
        binding.etSearch.doAfterTextChanged { text ->
            currentSearchQuery = text?.toString()?.trim().orEmpty().lowercase()
            applyFilters()
        }
    }

    private fun setupListeners() {
        binding.fabAddBook.setOnClickListener {
            val intent = BookFormActivity.newIntent(this)
            startActivity(intent)
        }

        binding.btnEmptyAddBook.setOnClickListener {
            val intent = BookFormActivity.newIntent(this)
            startActivity(intent)
        }
    }

    private fun loadBooks() {
        val userId = auth.currentUser?.uid ?: return
        allBooks = dbHelper.getAllBooks(userId)
        applyFilters()
    }

    private fun applyFilters() {
        var filtered = allBooks

        // Filtro por estado
        if (currentFilterStatus != null) {
            filtered = filtered.filter { it.status == currentFilterStatus }
        }

        // Filtro por búsqueda (título o autor)
        if (currentSearchQuery.isNotEmpty()) {
            filtered = filtered.filter {
                it.title.lowercase().contains(currentSearchQuery) ||
                it.author.lowercase().contains(currentSearchQuery)
            }
        }

        adapter.submitList(filtered)

        // Manejo de Estado Vacío
        if (filtered.isEmpty()) {
            binding.rvBooks.visibility = View.GONE
            binding.llEmptyState.visibility = View.VISIBLE

            if (allBooks.isEmpty()) {
                binding.tvEmptyTitle.text = "Tu biblioteca está vacía"
                binding.tvEmptySubtitle.text = "Aún no has registrado ningún libro. ¡Presiona el botón para agregar el primero!"
                binding.btnEmptyAddBook.visibility = View.VISIBLE
            } else {
                binding.tvEmptyTitle.text = "Sin resultados"
                binding.tvEmptySubtitle.text = "No se encontraron libros que coincidan con tus filtros de búsqueda."
                binding.btnEmptyAddBook.visibility = View.GONE
            }
        } else {
            binding.rvBooks.visibility = View.VISIBLE
            binding.llEmptyState.visibility = View.GONE
        }
    }
}
