package io.github.gonzily1269.book_management.service

import io.github.gonzily1269.book_management.dto.AuthorCreateRequest
import io.github.gonzily1269.book_management.dto.BookCreateRequest
import io.github.gonzily1269.book_management.dto.BookUpdateRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * BookServiceのテストクラス
 *
 * 書籍サービスの書籍作成・更新・検索機能とビジネスロジックをテストする。
 * リポジトリを経由してDB操作を実行する。
 */
@SpringBootTest
@Transactional
@DisplayName("BookServiceのテスト")
class BookServiceTest @Autowired constructor(
    private val bookService: BookService,
    private val authorService: AuthorService
) {

    private var testAuthorId = 0
    private var testAuthorId2 = 0

    /**
     * テストの初期化 - テスト用の著者を作成
     */
    @BeforeEach
    fun setUp() {
        val author1 = authorService.createAuthor(
            AuthorCreateRequest("太郎", LocalDate.of(1990, 1, 15))
        )
        val author2 = authorService.createAuthor(
            AuthorCreateRequest("花子", LocalDate.of(1985, 6, 20))
        )
        testAuthorId = author1.id!!
        testAuthorId2 = author2.id!!
    }

    @Test
    @DisplayName("書籍作成リクエストから書籍を作成できることをテストする")
    fun testCreateBook() {
        val request = createBookRequest("Spring Boot入門", 3000, "PUBLISHED", listOf(testAuthorId))
        val result = bookService.createBook(request)

        assertNotNull(result.id)
        assertEquals(request.title, result.title)
        assertEquals(request.price, result.price)
        assertEquals(request.publicationStatus, result.publicationStatus)
        assertEquals(1, result.authors.size)
    }

    @Test
    @DisplayName("複数の著者を持つ書籍を作成できることをテストする")
    fun testCreateBookWithMultipleAuthors() {
        val request = createBookRequest("Kotlin完全ガイド", 4000, "PUBLISHED", listOf(testAuthorId, testAuthorId2))
        val result = bookService.createBook(request)

        assertNotNull(result.id)
        assertEquals(2, result.authors.size)
    }

    @Test
    @DisplayName("著者IDで書籍を検索できることをテストする")
    fun testGetBooksByAuthorId() {
        val book1 = bookService.createBook(
            createBookRequest("本1", 1000, "PUBLISHED", listOf(testAuthorId))
        )
        val book2 = bookService.createBook(
            createBookRequest("本2", 2000, "UNPUBLISHED", listOf(testAuthorId))
        )
        bookService.createBook(
            createBookRequest("本3", 3000, "PUBLISHED", listOf(testAuthorId2))
        )

        val result = bookService.getBooksByAuthorId(testAuthorId)

        assertEquals(2, result.size)
        assertEquals(setOf(book1.id, book2.id), result.map { it.id }.toSet())
    }

    @Test
    @DisplayName("存在しない著者IDで書籍を検索すると空のリストが返されることをテストする")
    fun testGetBooksByNonExistentAuthorId() {
        val nonExistentId = 99999
        val result = bookService.getBooksByAuthorId(nonExistentId)
        assertEquals(0, result.size)
    }

    @Test
    @DisplayName("書籍情報を更新できることをテストする")
    fun testUpdateBook() {
        val createdBook = bookService.createBook(
            createBookRequest("元のタイトル", 1000, "UNPUBLISHED", listOf(testAuthorId))
        )
        val bookId = createdBook.id!!

        val updateRequest = updateBookRequest("新しいタイトル", 2000, "UNPUBLISHED", listOf(testAuthorId))
        val result = bookService.updateBook(bookId, updateRequest)

        assertNotNull(result)
        assertEquals(bookId, result.id)
        assertEquals("新しいタイトル", result.title)
        assertEquals(2000, result.price)
    }

    @Test
    @DisplayName("存在しない書籍を更新しようとするとnullが返されることをテストする")
    fun testUpdateNonExistentBook() {
        val nonExistentId = 99999
        val updateRequest = updateBookRequest("タイトル", 1000, "PUBLISHED", listOf(testAuthorId))
        val result = bookService.updateBook(nonExistentId, updateRequest)
        assertNull(result)
    }

    @Test
    @DisplayName("出版済みから未出版への変更ができないことをテストする")
    fun testCannotUnpublishPublishedBook() {
        val createdBook = bookService.createBook(
            createBookRequest("出版済み本", 3000, "PUBLISHED", listOf(testAuthorId))
        )
        val bookId = createdBook.id!!

        val updateRequest = updateBookRequest("出版済み本", 3000, "UNPUBLISHED", listOf(testAuthorId))

        assertThrows<IllegalStateException> {
            bookService.updateBook(bookId, updateRequest)
        }
    }

    @Test
    @DisplayName("未出版から出版への変更ができることをテストする")
    fun testCanPublishUnpublishedBook() {
        val createdBook = bookService.createBook(
            createBookRequest("未出版本", 2000, "UNPUBLISHED", listOf(testAuthorId))
        )
        val bookId = createdBook.id!!

        val updateRequest = updateBookRequest("未出版本", 2000, "PUBLISHED", listOf(testAuthorId))
        val result = bookService.updateBook(bookId, updateRequest)

        assertNotNull(result)
        assertEquals("PUBLISHED", result.publicationStatus)
    }

    @Test
    @DisplayName("出版状態を変更せずに更新できることをテストする")
    fun testUpdatePublishedBookWithoutChangingStatus() {
        val createdBook = bookService.createBook(
            createBookRequest("出版済み本", 1000, "PUBLISHED", listOf(testAuthorId))
        )
        val bookId = createdBook.id!!

        val updateRequest = updateBookRequest("新しいタイトル", 3000, "PUBLISHED", listOf(testAuthorId))
        val result = bookService.updateBook(bookId, updateRequest)

        assertNotNull(result)
        assertEquals("新しいタイトル", result.title)
        assertEquals(3000, result.price)
        assertEquals("PUBLISHED", result.publicationStatus)
    }

    @Test
    @DisplayName("複数の書籍を作成・検索できることをテストする")
    fun testCreateAndGetMultipleBooks() {
        val book1 = bookService.createBook(
            createBookRequest("本1", 1000, "PUBLISHED", listOf(testAuthorId))
        )
        val book2 = bookService.createBook(
            createBookRequest("本2", 2000, "UNPUBLISHED", listOf(testAuthorId))
        )

        val result = bookService.getBooksByAuthorId(testAuthorId)

        assertEquals(2, result.size)
        assertEquals(setOf(book1.id, book2.id), result.map { it.id }.toSet())
    }

    @Test
    @DisplayName("書籍の著者を変更できることをテストする")
    fun testUpdateBookAuthors() {
        val createdBook = bookService.createBook(
            createBookRequest("本", 1000, "PUBLISHED", listOf(testAuthorId))
        )
        val bookId = createdBook.id!!

        val updateRequest = updateBookRequest("本", 1000, "PUBLISHED", listOf(testAuthorId2))
        val result = bookService.updateBook(bookId, updateRequest)

        assertNotNull(result)
        assertEquals(1, result.authors.size)
        assertEquals(testAuthorId2, result.authors[0].id)
    }

    private fun createBookRequest(
        title: String,
        price: Int,
        publicationStatus: String,
        authorIds: List<Int>
    ) = BookCreateRequest(
        title = title,
        price = price,
        publicationStatus = publicationStatus,
        authorIds = authorIds
    )

    private fun updateBookRequest(
        title: String,
        price: Int,
        publicationStatus: String,
        authorIds: List<Int>
    ) = BookUpdateRequest(
        title = title,
        price = price,
        authorIds = authorIds,
        publicationStatus = publicationStatus
    )
}

