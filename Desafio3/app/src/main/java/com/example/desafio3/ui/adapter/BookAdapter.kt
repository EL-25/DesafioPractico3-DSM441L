package com.example.desafio3.ui.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.desafio3.R
import com.example.desafio3.data.model.Book
import com.example.desafio3.data.model.Genre
import com.example.desafio3.data.model.ReadingStatus
import com.example.desafio3.databinding.ItemBookBinding

class BookAdapter(
    private val onBookClick: (Book) -> Unit
) : ListAdapter<Book, BookAdapter.BookViewHolder>(BookDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val binding = ItemBookBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BookViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class BookViewHolder(private val binding: ItemBookBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(book: Book) {
            val context = binding.root.context

            binding.tvTitle.text = book.title
            binding.tvAuthor.text = book.author
            binding.tvGenreChip.text = book.genre
            binding.tvStatusChip.text = book.status

            // Configurar color de género
            val (genreTextColor, genreBgColor) = when (book.genre) {
                Genre.NOVELA -> Pair(R.color.genre_novela, R.color.genre_novela_bg)
                Genre.CIENCIA_FICCION -> Pair(R.color.genre_scifi, R.color.genre_scifi_bg)
                Genre.HISTORIA -> Pair(R.color.genre_historia, R.color.genre_historia_bg)
                Genre.DESARROLLO_PERSONAL -> Pair(R.color.genre_desarrollo, R.color.genre_desarrollo_bg)
                else -> Pair(R.color.primary, R.color.primary_container)
            }
            binding.tvGenreChip.setTextColor(ContextCompat.getColor(context, genreTextColor))
            binding.tvGenreChip.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, genreBgColor))

            // Configurar color de estado
            val (statusTextColor, statusBgColor) = when (book.status) {
                ReadingStatus.POR_LEER -> Pair(R.color.status_por_leer, R.color.status_por_leer_bg)
                ReadingStatus.LEYENDO -> Pair(R.color.status_leyendo, R.color.status_leyendo_bg)
                ReadingStatus.TERMINADO -> Pair(R.color.status_terminado, R.color.status_terminado_bg)
                else -> Pair(R.color.text_secondary, R.color.app_card_stroke)
            }
            binding.tvStatusChip.setTextColor(ContextCompat.getColor(context, statusTextColor))
            binding.tvStatusChip.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, statusBgColor))

            // Progreso
            val percentage = book.progressPercentage
            binding.tvProgressPages.text = "${book.readPages} / ${book.totalPages} páginas"
            binding.tvProgressPercentage.text = "$percentage%"
            binding.progressBar.progress = percentage

            // Calificación (mostrar SOLO si el libro está terminado)
            if (book.isFinished) {
                binding.llRatingContainer.visibility = View.VISIBLE
                binding.tvRatingText.text = "${book.rating} / 5 estrellas"
                if (!book.finishDate.isNullOrEmpty()) {
                    binding.tvFinishDate.visibility = View.VISIBLE
                    binding.tvFinishDate.text = "Terminado: ${book.finishDate}"
                } else {
                    binding.tvFinishDate.visibility = View.GONE
                }
            } else {
                binding.llRatingContainer.visibility = View.GONE
            }

            // Click listener
            binding.root.setOnClickListener {
                onBookClick(book)
            }
        }
    }

    class BookDiffCallback : DiffUtil.ItemCallback<Book>() {
        override fun areItemsTheSame(oldItem: Book, newItem: Book): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Book, newItem: Book): Boolean = oldItem == newItem
    }
}
