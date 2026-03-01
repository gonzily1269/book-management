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
        // Given: 書籍作成リクエストを準備
        val request = BookCreateRequest(
            title = "Spring Boot入門",
            price = 3000,
            authorIds = listOf(testAuthorId),
            publicationStatus = "PUBLISHED"
        )

        // When: 書籍を作成
        val result = bookService.createBook(request)

        // Then: 書籍が正常に作成されたことを検証
        assertNotNull(result.id)
        assertEquals(request.title, result.title)
        assertEquals(request.price, result.price)
        assertEquals(request.publicationStatus, result.publicationStatus)
        assertEquals(1, result.authors.size)
    }

    @Test
    @DisplayName("複数の著者を持つ書籍を作成できることをテストする")
    fun testCreateBookWithMultipleAuthors() {
        // Given: 複数の著者を持つ書籍作成リクエスト
        val request = BookCreateRequest(
            title = "Kotlin完全ガイド",
            price = 4000,
            authorIds = listOf(testAuthorId, testAuthorId2),
            publicationStatus = "PUBLISHED"
        )

        // When: 書籍を作成
        val result = bookService.createBook(request)

        // Then: 複数の著者を持つ書籍が正常に作成されたことを検証
        assertNotNull(result.id)
        assertEquals(2, result.authors.size)
    }

    @Test
    @DisplayName("著者IDで書籍を検索できることをテストする")
    fun testGetBooksByAuthorId() {
        // Given: テスト著者で複数の書籍を作成
        val book1 = bookService.createBook(
            BookCreateRequest("本1", 1000, "PUBLISHED", listOf(testAuthorId))
        )
        val book2 = bookService.createBook(
            BookCreateRequest("本2", 2000, "UNPUBLISHED", listOf(testAuthorId))
        )
        bookService.createBook(
            BookCreateRequest("本3", 3000, "PUBLISHED", listOf(testAuthorId2))
        )

        // When: テスト著者で書籍を検索
        val result = bookService.getBooksByAuthorId(testAuthorId)

        // Then: テスト著者の書籍のみが返されることを検証
        assertEquals(2, result.size)
        assertEquals(setOf(book1.id, book2.id), result.map { it.id }.toSet())
    }

    @Test
    @DisplayName("存在しない著者IDで書籍を検索すると空のリストが返されることをテストする")
    fun testGetBooksByNonExistentAuthorId() {
        // Given: 存在しないID
        val nonExistentId = 99999

        // When: 存在しない著者で書籍を検索
        val result = bookService.getBooksByAuthorId(nonExistentId)

        // Then: 空のリストが返されることを検証
        assertEquals(0, result.size)
    }

    @Test
    @DisplayName("書籍情報を更新できることをテストする")
    fun testUpdateBook() {
        // Given: 書籍を作成してから更新リクエストを準備
        val createdBook = bookService.createBook(
            BookCreateRequest("元のタイトル", 1000, "UNPUBLISHED", listOf(testAuthorId))
        )
        val bookId = createdBook.id!!

        val updateRequest = BookUpdateRequest(
            title = "新しいタイトル",
            price = 2000,
            authorIds = listOf(testAuthorId),
            publicationStatus = "UNPUBLISHED"
        )

        // When: 書籍情報を更新
        val result = bookService.updateBook(bookId, updateRequest)

        // Then: 書籍情報が正常に更新されたことを検証
        assertNotNull(result)
        assertEquals(bookId, result.id)
        assertEquals("新しいタイトル", result.title)
        assertEquals(2000, result.price)
    }

    @Test
    @DisplayName("存在しない書籍を更新しようとするとnullが返されることをテストする")
    fun testUpdateNonExistentBook() {
        // Given: 存在しないID
        val nonExistentId = 99999
        val updateRequest = BookUpdateRequest(
            title = "タイトル",
            price = 1000,
            authorIds = listOf(testAuthorId),
            publicationStatus = "PUBLISHED"
        )

        // When: 存在しない書籍を更新しようとする
        val result = bookService.updateBook(nonExistentId, updateRequest)

        // Then: nullが返されることを検証
        assertNull(result)
    }

    @Test
    @DisplayName("出版済みから未出版への変更ができないことをテストする")
    fun testCannotUnpublishPublishedBook() {
        // Given: 出版済みの書籍を作成
        val createdBook = bookService.createBook(
            BookCreateRequest("出版済み本", 3000, "PUBLISHED", listOf(testAuthorId))
        )
        val bookId = createdBook.id!!

        val updateRequest = BookUpdateRequest(
            title = "出版済み本",
            price = 3000,
            authorIds = listOf(testAuthorId),
            publicationStatus = "UNPUBLISHED"  // 未出版に変更しようとする
        )

        // When & Then: 出版済みから未出版への変更を試みるとIllegalStateExceptionがスロー
        assertThrows<IllegalStateException> {
            bookService.updateBook(bookId, updateRequest)
        }
    }

    @Test
    @DisplayName("未出版から出版への変更ができることをテストする")
    fun testCanPublishUnpublishedBook() {
        // Given: 未出版の書籍を作成
        val createdBook = bookService.createBook(
            BookCreateRequest("未出版本", 2000, "UNPUBLISHED", listOf(testAuthorId))
        )
        val bookId = createdBook.id!!

        val updateRequest = BookUpdateRequest(
            title = "未出版本",
            price = 2000,
            authorIds = listOf(testAuthorId),
            publicationStatus = "PUBLISHED"  // 出版に変更
        )

        // When: 未出版から出版への変更を実行
        val result = bookService.updateBook(bookId, updateRequest)

        // Then: 出版状態が変更されたことを検証
        assertNotNull(result)
        assertEquals("PUBLISHED", result.publicationStatus)
    }

    @Test
    @DisplayName("出版状態を変更せずに更新できることをテストする")
    fun testUpdatePublishedBookWithoutChangingStatus() {
        // Given: 出版済みの書籍を作成
        val createdBook = bookService.createBook(
            BookCreateRequest("出版済み本", 1000, "PUBLISHED", listOf(testAuthorId))
        )
        val bookId = createdBook.id!!

        val updateRequest = BookUpdateRequest(
            title = "新しいタイトル",
            price = 3000,
            authorIds = listOf(testAuthorId),
            publicationStatus = "PUBLISHED"  // 出版状態は変更しない
        )

        // When: 出版状態を変更せずに更新
        val result = bookService.updateBook(bookId, updateRequest)

        // Then: 他の情報は更新されたことを検証
        assertNotNull(result)
        assertEquals("新しいタイトル", result.title)
        assertEquals(3000, result.price)
        assertEquals("PUBLISHED", result.publicationStatus)
    }

    @Test
    @DisplayName("複数の書籍を作成・検索できることをテストする")
    fun testCreateAndGetMultipleBooks() {
        // Given: 複数の書籍を作成
        val book1 = bookService.createBook(
            BookCreateRequest("本1", 1000, "PUBLISHED", listOf(testAuthorId))
        )
        val book2 = bookService.createBook(
            BookCreateRequest("本2", 2000, "UNPUBLISHED", listOf(testAuthorId))
        )

        // When: 著者IDで書籍を検索
        val result = bookService.getBooksByAuthorId(testAuthorId)

        // Then: 両方の書籍が返されることを検証
        assertEquals(2, result.size)
        assertEquals(setOf(book1.id, book2.id), result.map { it.id }.toSet())
    }

    @Test
    @DisplayName("書籍の著者を変更できることをテストする")
    fun testUpdateBookAuthors() {
        // Given: テスト著者1で書籍を作成してからテスト著者2に変更
        val createdBook = bookService.createBook(
            BookCreateRequest("本", 1000, "PUBLISHED", listOf(testAuthorId))
        )
        val bookId = createdBook.id!!

        val updateRequest = BookUpdateRequest(
            title = "本",
            price = 1000,
            authorIds = listOf(testAuthorId2),  // 著者を変更
            publicationStatus = "PUBLISHED"
        )

        // When: 著者を変更
        val result = bookService.updateBook(bookId, updateRequest)

        // Then: 著者が変更されたことを検証
        assertNotNull(result)
        assertEquals(1, result?.authors?.size)
        assertEquals(testAuthorId2, result?.authors?.get(0)?.id)
    }
}

