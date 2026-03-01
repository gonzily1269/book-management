package io.github.gonzily1269.book_management.repository

import io.github.gonzily1269.book_management.dto.AuthorDto
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * BookRepositoryのテストクラス
 *
 * 書籍リポジトリの書籍作成・更新・検索機能をテストする。
 * 実環境と同じPostgreSQLを使用してDB操作をテストする。
 */
@SpringBootTest
@Transactional
@DisplayName("BookRepositoryのテスト")
class BookRepositoryTest @Autowired constructor(
    private val bookRepository: BookRepository,
    private val authorRepository: AuthorRepository
) {

    private lateinit var testAuthor: AuthorDto
    private lateinit var testAuthor2: AuthorDto

    /**
     * テストの初期化 - テスト用の著者を作成
     */
    @BeforeEach
    fun setUp() {
        testAuthor = authorRepository.create("太郎", LocalDate.of(1990, 1, 15))
        testAuthor2 = authorRepository.create("花子", LocalDate.of(1985, 6, 20))
    }

    private fun createBook(
        title: String,
        price: Int,
        authorIds: List<Int>,
        publicationStatus: String
    ) = bookRepository.create(title, price, authorIds, publicationStatus)

    private fun updateBook(
        id: Int,
        title: String,
        price: Int,
        publicationStatus: String,
        authorIds: List<Int>
    ) = bookRepository.update(id, title, price, publicationStatus, authorIds)

    @Test
    @DisplayName("書籍を正常に作成できることをテストする")
    fun testCreateBook() {
        val title = "Spring Boot入門"
        val price = 3000
        val authorIds = listOf(testAuthor.id!!)
        val publicationStatus = "PUBLISHED"

        val result = createBook(title, price, authorIds, publicationStatus)

        assertNotNull(result.id)
        assertEquals(title, result.title)
        assertEquals(price, result.price)
        assertEquals(publicationStatus, result.publicationStatus)
        assertEquals(1, result.authors.size)
        assertEquals(testAuthor.id, result.authors[0].id)
    }

    @Test
    @DisplayName("複数の著者を持つ書籍を作成できることをテストする")
    fun testCreateBookWithMultipleAuthors() {
        val title = "Kotlin完全ガイド"
        val price = 4000
        val authorIds = listOf(testAuthor.id!!, testAuthor2.id!!)
        val publicationStatus = "PUBLISHED"

        val result = createBook(title, price, authorIds, publicationStatus)

        assertNotNull(result.id)
        assertEquals(title, result.title)
        assertEquals(2, result.authors.size)
        assertTrue(result.authors.map { it.id }.contains(testAuthor.id))
        assertTrue(result.authors.map { it.id }.contains(testAuthor2.id))
    }

    @Test
    @DisplayName("IDで書籍を検索できることをテストする")
    fun testFindBookById() {
        val createdBook = createBook("Kotlin入門", 2000, listOf(testAuthor.id!!), "PUBLISHED")
        val bookId = createdBook.id!!

        val result = bookRepository.findById(bookId)

        assertNotNull(result)
        assertEquals(bookId, result.id)
        assertEquals("Kotlin入門", result.title)
        assertEquals(2000, result.price)
    }

    @Test
    @DisplayName("存在しない書籍をIDで検索するとnullが返されることをテストする")
    fun testFindNonExistentBookById() {
        val nonExistentId = 99999
        val result = bookRepository.findById(nonExistentId)
        assertNull(result)
    }

    @Test
    @DisplayName("著者IDで書籍を検索できることをテストする")
    fun testFindBooksByAuthorId() {
        val book1 = createBook("本1", 1000, listOf(testAuthor.id!!), "PUBLISHED")
        val book2 = createBook("本2", 2000, listOf(testAuthor.id!!), "UNPUBLISHED")
        createBook("本3", 3000, listOf(testAuthor2.id!!), "PUBLISHED")

        val result = bookRepository.findByAuthorId(testAuthor.id!!)

        assertEquals(2, result.size)
        assertTrue(result.map { it.id }.contains(book1.id))
        assertTrue(result.map { it.id }.contains(book2.id))
    }

    @Test
    @DisplayName("複数の著者を持つ書籍を著者IDで検索できることをテストする")
    fun testFindBooksWithMultipleAuthorsByAuthorId() {
        val book = createBook("共著本", 3000, listOf(testAuthor.id!!, testAuthor2.id!!), "PUBLISHED")

        val resultByAuthor1 = bookRepository.findByAuthorId(testAuthor.id!!)
        val resultByAuthor2 = bookRepository.findByAuthorId(testAuthor2.id!!)

        assertTrue(resultByAuthor1.map { it.id }.contains(book.id))
        assertTrue(resultByAuthor2.map { it.id }.contains(book.id))
    }

    @Test
    @DisplayName("書籍情報を正常に更新できることをテストする")
    fun testUpdateBook() {
        val book = createBook("元のタイトル", 1000, listOf(testAuthor.id!!), "UNPUBLISHED")
        val bookId = book.id!!

        val result = updateBook(
            bookId,
            "新しいタイトル",
            2000,
            "PUBLISHED",
            listOf(testAuthor.id!!)
        )

        assertNotNull(result)
        assertEquals(bookId, result.id)
        assertEquals("新しいタイトル", result.title)
        assertEquals(2000, result.price)
        assertEquals("PUBLISHED", result.publicationStatus)
    }

    @Test
    @DisplayName("書籍の著者を変更できることをテストする")
    fun testUpdateBookAuthors() {
        val book = createBook("本", 1000, listOf(testAuthor.id!!), "PUBLISHED")
        val bookId = book.id!!

        val result = updateBook(
            bookId,
            book.title,
            book.price,
            book.publicationStatus,
            listOf(testAuthor2.id!!)
        )

        assertNotNull(result)
        assertEquals(1, result.authors.size)
        assertEquals(testAuthor2.id, result.authors[0].id)
    }

    @Test
    @DisplayName("複数の著者を1人に減らせることをテストする")
    fun testUpdateBookRemoveAuthor() {
        val book = createBook("共著本", 3000, listOf(testAuthor.id!!, testAuthor2.id!!), "PUBLISHED")
        val bookId = book.id!!

        val result = updateBook(
            bookId,
            book.title,
            book.price,
            book.publicationStatus,
            listOf(testAuthor.id!!)
        )

        assertNotNull(result)
        assertEquals(1, result.authors.size)
        assertEquals(testAuthor.id, result.authors[0].id)
    }

    @Test
    @DisplayName("1人の著者を複数人に増やせることをテストする")
    fun testUpdateBookAddAuthor() {
        val book = createBook("本", 1000, listOf(testAuthor.id!!), "PUBLISHED")
        val bookId = book.id!!

        val result = updateBook(
            bookId,
            book.title,
            book.price,
            book.publicationStatus,
            listOf(testAuthor.id!!, testAuthor2.id!!)
        )

        assertNotNull(result)
        assertEquals(2, result.authors.size)
    }

    @Test
    @DisplayName("存在しない書籍を更新しようとするとnullが返されることをテストする")
    fun testUpdateNonExistentBook() {
        val nonExistentId = 99999
        val result = updateBook(
            nonExistentId,
            "タイトル",
            1000,
            "PUBLISHED",
            listOf(testAuthor.id!!)
        )
        assertNull(result)
    }

    @Test
    @DisplayName("複数の書籍を作成・検索できることをテストする")
    fun testCreateAndFindMultipleBooks() {
        val book1 = createBook("本1", 1000, listOf(testAuthor.id!!), "PUBLISHED")
        val book2 = createBook("本2", 2000, listOf(testAuthor.id!!), "UNPUBLISHED")

        val foundBook1 = bookRepository.findById(book1.id!!)
        val foundBook2 = bookRepository.findById(book2.id!!)

        assertNotNull(foundBook1)
        assertNotNull(foundBook2)
        assertEquals("本1", foundBook1.title)
        assertEquals("本2", foundBook2.title)
    }
}

