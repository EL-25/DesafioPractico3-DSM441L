package com.example.desafio3.data.model

data class Book(
    val id: Long = 0,
    val userId: String,
    val title: String,
    val author: String,
    val genre: String,
    val totalPages: Int,
    val readPages: Int,
    val status: String,
    val rating: Int = 0,
    val finishDate: String? = null,
    val finishYear: Int? = null,
    val finishTimestamp: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val progressPercentage: Int
        get() = if (totalPages > 0) {
            ((readPages.toDouble() / totalPages.toDouble()) * 100).toInt().coerceIn(0, 100)
        } else {
            0
        }

    val isFinished: Boolean
        get() = status == ReadingStatus.TERMINADO
}
