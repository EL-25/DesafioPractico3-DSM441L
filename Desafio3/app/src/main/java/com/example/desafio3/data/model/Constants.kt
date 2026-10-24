package com.example.desafio3.data.model

object Genre {
    const val NOVELA = "Novela"
    const val CIENCIA_FICCION = "Ciencia ficción"
    const val HISTORIA = "Historia"
    const val DESARROLLO_PERSONAL = "Desarrollo personal"

    val ALL = listOf(NOVELA, CIENCIA_FICCION, HISTORIA, DESARROLLO_PERSONAL)
}

object ReadingStatus {
    const val POR_LEER = "Por leer"
    const val LEYENDO = "Leyendo"
    const val TERMINADO = "Terminado"

    val ALL = listOf(POR_LEER, LEYENDO, TERMINADO)
}
