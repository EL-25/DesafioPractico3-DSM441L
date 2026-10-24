package com.example.desafio3.ui.stats

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.desafio3.data.database.BookDatabaseHelper
import com.example.desafio3.data.model.Genre
import com.example.desafio3.databinding.ActivityStatsBinding
import com.google.firebase.auth.FirebaseAuth
import java.util.Calendar

class StatsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStatsBinding
    private lateinit var dbHelper: BookDatabaseHelper
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = BookDatabaseHelper(this)
        auth = FirebaseAuth.getInstance()

        setupToolbar()
        loadStatistics()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun loadStatistics() {
        val userId = auth.currentUser?.uid ?: return
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        binding.tvCurrentYearTitle.text = "Estadísticas del Año $currentYear"

        // 1. Libros terminados en el año actual por cada uno de los cuatro géneros
        val genreCounts = dbHelper.getFinishedBooksCountByGenreInYear(userId, currentYear)
        val novelaCount = genreCounts[Genre.NOVELA] ?: 0
        val scifiCount = genreCounts[Genre.CIENCIA_FICCION] ?: 0
        val historiaCount = genreCounts[Genre.HISTORIA] ?: 0
        val desarrolloCount = genreCounts[Genre.DESARROLLO_PERSONAL] ?: 0

        val totalFinished = novelaCount + scifiCount + historiaCount + desarrolloCount

        binding.tvCountNovela.text = "$novelaCount ${if (novelaCount == 1) "libro" else "libros"}"
        binding.tvCountSciFi.text = "$scifiCount ${if (scifiCount == 1) "libro" else "libros"}"
        binding.tvCountHistoria.text = "$historiaCount ${if (historiaCount == 1) "libro" else "libros"}"
        binding.tvCountDesarrollo.text = "$desarrolloCount ${if (desarrolloCount == 1) "libro" else "libros"}"

        // Porcentajes de barras
        val maxInAny = maxOf(novelaCount, scifiCount, historiaCount, desarrolloCount, 1)
        binding.progressNovela.progress = ((novelaCount.toDouble() / maxInAny.toDouble()) * 100).toInt()
        binding.progressSciFi.progress = ((scifiCount.toDouble() / maxInAny.toDouble()) * 100).toInt()
        binding.progressHistoria.progress = ((historiaCount.toDouble() / maxInAny.toDouble()) * 100).toInt()
        binding.progressDesarrollo.progress = ((desarrolloCount.toDouble() / maxInAny.toDouble()) * 100).toInt()

        // 2. Género más leído en el año
        val mostReadGenre = dbHelper.getMostReadGenreInYear(userId, currentYear)
        if (mostReadGenre != null) {
            val count = genreCounts[mostReadGenre] ?: 0
            binding.tvMostReadGenre.text = mostReadGenre
            binding.tvMostReadGenreCount.text = "$count ${if (count == 1) "libro terminado" else "libros terminados"}"
        } else {
            binding.tvMostReadGenre.text = "Sin lecturas"
            binding.tvMostReadGenreCount.text = "0 libros terminados"
        }

        // 3. Total de páginas leídas en el año
        val totalPages = dbHelper.getTotalPagesReadInYear(userId, currentYear)
        binding.tvTotalPagesRead.text = String.format("%,d", totalPages)

        // 4. Libro mejor calificado del año (en caso de empate, el que se terminó primero)
        val bestBook = dbHelper.getBestRatedBookInYear(userId, currentYear)
        if (bestBook != null) {
            binding.llBestBookContent.visibility = View.VISIBLE
            binding.tvBestBookEmpty.visibility = View.GONE

            binding.tvBestBookTitle.text = bestBook.title
            binding.tvBestBookAuthor.text = "por ${bestBook.author}"
            binding.tvBestBookRating.text = "${bestBook.rating} / 5 estrellas"
            val finishDateStr = if (!bestBook.finishDate.isNullOrEmpty()) bestBook.finishDate else "Año $currentYear"
            binding.tvBestBookFinishDate.text = "Terminado el $finishDateStr"
        } else {
            binding.llBestBookContent.visibility = View.GONE
            binding.tvBestBookEmpty.visibility = View.VISIBLE
        }
    }
}
