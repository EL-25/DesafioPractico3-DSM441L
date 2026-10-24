package com.example.desafio3.data.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.desafio3.data.model.Book
import com.example.desafio3.data.model.Genre
import com.example.desafio3.data.model.ReadingStatus

class BookDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "desafio3_books.db"
        private const val DATABASE_VERSION = 1

        // Table Books
        const val TABLE_BOOKS = "books"
        const val COL_ID = "id"
        const val COL_USER_ID = "user_id"
        const val COL_TITLE = "title"
        const val COL_AUTHOR = "author"
        const val COL_GENRE = "genre"
        const val COL_TOTAL_PAGES = "total_pages"
        const val COL_READ_PAGES = "read_pages"
        const val COL_STATUS = "status"
        const val COL_RATING = "rating"
        const val COL_FINISH_DATE = "finish_date"
        const val COL_FINISH_YEAR = "finish_year"
        const val COL_FINISH_TIMESTAMP = "finish_timestamp"
        const val COL_CREATED_AT = "created_at"

        // Table Annual Goals
        const val TABLE_GOALS = "annual_goals"
        const val COL_GOAL_ID = "id"
        const val COL_GOAL_USER_ID = "user_id"
        const val COL_GOAL_YEAR = "year"
        const val COL_GOAL_TARGET = "target_goal"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createBooksTable = """
            CREATE TABLE $TABLE_BOOKS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_USER_ID TEXT NOT NULL,
                $COL_TITLE TEXT NOT NULL,
                $COL_AUTHOR TEXT NOT NULL,
                $COL_GENRE TEXT NOT NULL,
                $COL_TOTAL_PAGES INTEGER NOT NULL,
                $COL_READ_PAGES INTEGER NOT NULL,
                $COL_STATUS TEXT NOT NULL,
                $COL_RATING INTEGER NOT NULL DEFAULT 0,
                $COL_FINISH_DATE TEXT,
                $COL_FINISH_YEAR INTEGER,
                $COL_FINISH_TIMESTAMP INTEGER,
                $COL_CREATED_AT INTEGER NOT NULL
            )
        """.trimIndent()

        val createGoalsTable = """
            CREATE TABLE $TABLE_GOALS (
                $COL_GOAL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_GOAL_USER_ID TEXT NOT NULL,
                $COL_GOAL_YEAR INTEGER NOT NULL,
                $COL_GOAL_TARGET INTEGER NOT NULL,
                UNIQUE($COL_GOAL_USER_ID, $COL_GOAL_YEAR) ON CONFLICT REPLACE
            )
        """.trimIndent()

        db.execSQL(createBooksTable)
        db.execSQL(createGoalsTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_BOOKS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_GOALS")
        onCreate(db)
    }

    // ================= BOOK OPERATIONS =================

    fun insertBook(book: Book): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_USER_ID, book.userId)
            put(COL_TITLE, book.title.trim())
            put(COL_AUTHOR, book.author.trim())
            put(COL_GENRE, book.genre)
            put(COL_TOTAL_PAGES, book.totalPages)
            put(COL_READ_PAGES, book.readPages)
            put(COL_STATUS, book.status)
            put(COL_RATING, book.rating)
            put(COL_FINISH_DATE, book.finishDate)
            put(COL_FINISH_YEAR, book.finishYear)
            put(COL_FINISH_TIMESTAMP, book.finishTimestamp)
            put(COL_CREATED_AT, book.createdAt)
        }
        return db.insert(TABLE_BOOKS, null, values)
    }

    fun updateBook(book: Book): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_TITLE, book.title.trim())
            put(COL_AUTHOR, book.author.trim())
            put(COL_GENRE, book.genre)
            put(COL_TOTAL_PAGES, book.totalPages)
            put(COL_READ_PAGES, book.readPages)
            put(COL_STATUS, book.status)
            put(COL_RATING, book.rating)
            put(COL_FINISH_DATE, book.finishDate)
            put(COL_FINISH_YEAR, book.finishYear)
            put(COL_FINISH_TIMESTAMP, book.finishTimestamp)
        }
        return db.update(
            TABLE_BOOKS,
            values,
            "$COL_ID = ? AND $COL_USER_ID = ?",
            arrayOf(book.id.toString(), book.userId)
        )
    }

    fun deleteBook(bookId: Long, userId: String): Int {
        val db = writableDatabase
        return db.delete(
            TABLE_BOOKS,
            "$COL_ID = ? AND $COL_USER_ID = ?",
            arrayOf(bookId.toString(), userId)
        )
    }

    fun getBookById(bookId: Long, userId: String): Book? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_BOOKS,
            null,
            "$COL_ID = ? AND $COL_USER_ID = ?",
            arrayOf(bookId.toString(), userId),
            null, null, null
        )
        return cursor.use {
            if (it.moveToFirst()) cursorToBook(it) else null
        }
    }

    fun getAllBooks(userId: String): List<Book> {
        val books = mutableListOf<Book>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_BOOKS,
            null,
            "$COL_USER_ID = ?",
            arrayOf(userId),
            null, null,
            "$COL_ID DESC"
        )
        cursor.use {
            while (it.moveToNext()) {
                books.add(cursorToBook(it))
            }
        }
        return books
    }

    fun getCurrentlyReadingBooks(userId: String): List<Book> {
        val books = mutableListOf<Book>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_BOOKS,
            null,
            "$COL_USER_ID = ? AND $COL_STATUS = ?",
            arrayOf(userId, ReadingStatus.LEYENDO),
            null, null,
            "$COL_ID DESC"
        )
        cursor.use {
            while (it.moveToNext()) {
                books.add(cursorToBook(it))
            }
        }
        return books
    }

    // ================= ANNUAL GOAL OPERATIONS =================

    fun getAnnualGoal(userId: String, year: Int): Int {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_GOALS,
            arrayOf(COL_GOAL_TARGET),
            "$COL_GOAL_USER_ID = ? AND $COL_GOAL_YEAR = ?",
            arrayOf(userId, year.toString()),
            null, null, null
        )
        return cursor.use {
            if (it.moveToFirst()) it.getInt(0) else 10 // default 10 if not set
        }
    }

    fun setAnnualGoal(userId: String, year: Int, target: Int): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_GOAL_USER_ID, userId)
            put(COL_GOAL_YEAR, year)
            put(COL_GOAL_TARGET, target)
        }
        val result = db.insertWithOnConflict(
            TABLE_GOALS,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
        return result != -1L
    }

    // ================= STATISTICS OPERATIONS =================

    fun getFinishedBooksCountInYear(userId: String, year: Int): Int {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_BOOKS WHERE $COL_USER_ID = ? AND $COL_STATUS = ? AND $COL_FINISH_YEAR = ?",
            arrayOf(userId, ReadingStatus.TERMINADO, year.toString())
        )
        return cursor.use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
    }

    fun getFinishedBooksCountByGenreInYear(userId: String, year: Int): Map<String, Int> {
        val result = mutableMapOf(
            Genre.NOVELA to 0,
            Genre.CIENCIA_FICCION to 0,
            Genre.HISTORIA to 0,
            Genre.DESARROLLO_PERSONAL to 0
        )
        val db = readableDatabase
        val cursor = db.rawQuery(
            """
            SELECT $COL_GENRE, COUNT(*) 
            FROM $TABLE_BOOKS 
            WHERE $COL_USER_ID = ? AND $COL_STATUS = ? AND $COL_FINISH_YEAR = ? 
            GROUP BY $COL_GENRE
            """.trimIndent(),
            arrayOf(userId, ReadingStatus.TERMINADO, year.toString())
        )
        cursor.use {
            while (it.moveToNext()) {
                val genre = it.getString(0)
                val count = it.getInt(1)
                result[genre] = count
            }
        }
        return result
    }

    fun getMostReadGenreInYear(userId: String, year: Int): String? {
        val counts = getFinishedBooksCountByGenreInYear(userId, year)
        val maxEntry = counts.entries.maxByOrNull { it.value }
        return if (maxEntry != null && maxEntry.value > 0) {
            maxEntry.key
        } else {
            null
        }
    }

    fun getTotalPagesReadInYear(userId: String, year: Int): Int {
        val db = readableDatabase
        // Total pages read = read_pages of books finished in this year + read_pages of active books
        val cursor = db.rawQuery(
            """
            SELECT SUM($COL_READ_PAGES) 
            FROM $TABLE_BOOKS 
            WHERE $COL_USER_ID = ? AND (
                ($COL_STATUS = ? AND $COL_FINISH_YEAR = ?) OR 
                ($COL_STATUS = ?)
            )
            """.trimIndent(),
            arrayOf(userId, ReadingStatus.TERMINADO, year.toString(), ReadingStatus.LEYENDO)
        )
        return cursor.use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
    }

    fun getBestRatedBookInYear(userId: String, year: Int): Book? {
        val db = readableDatabase
        // In case of tie, sort by finish_timestamp ASC (the one finished first)
        val cursor = db.rawQuery(
            """
            SELECT * FROM $TABLE_BOOKS 
            WHERE $COL_USER_ID = ? AND $COL_STATUS = ? AND $COL_FINISH_YEAR = ? 
            ORDER BY $COL_RATING DESC, $COL_FINISH_TIMESTAMP ASC, $COL_ID ASC 
            LIMIT 1
            """.trimIndent(),
            arrayOf(userId, ReadingStatus.TERMINADO, year.toString())
        )
        return cursor.use {
            if (it.moveToFirst()) cursorToBook(it) else null
        }
    }

    private fun cursorToBook(cursor: android.database.Cursor): Book {
        return Book(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)),
            userId = cursor.getString(cursor.getColumnIndexOrThrow(COL_USER_ID)),
            title = cursor.getString(cursor.getColumnIndexOrThrow(COL_TITLE)),
            author = cursor.getString(cursor.getColumnIndexOrThrow(COL_AUTHOR)),
            genre = cursor.getString(cursor.getColumnIndexOrThrow(COL_GENRE)),
            totalPages = cursor.getInt(cursor.getColumnIndexOrThrow(COL_TOTAL_PAGES)),
            readPages = cursor.getInt(cursor.getColumnIndexOrThrow(COL_READ_PAGES)),
            status = cursor.getString(cursor.getColumnIndexOrThrow(COL_STATUS)),
            rating = cursor.getInt(cursor.getColumnIndexOrThrow(COL_RATING)),
            finishDate = cursor.getString(cursor.getColumnIndexOrThrow(COL_FINISH_DATE)),
            finishYear = if (cursor.isNull(cursor.getColumnIndexOrThrow(COL_FINISH_YEAR))) null else cursor.getInt(cursor.getColumnIndexOrThrow(COL_FINISH_YEAR)),
            finishTimestamp = if (cursor.isNull(cursor.getColumnIndexOrThrow(COL_FINISH_TIMESTAMP))) null else cursor.getLong(cursor.getColumnIndexOrThrow(COL_FINISH_TIMESTAMP)),
            createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COL_CREATED_AT))
        )
    }
}
