package io.github.gonzily1269.book_management.repository

import io.github.gonzily1269.book_management.dto.AuthorDto
import io.github.gonzily1269.tables.Book
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

    @Test
    @DisplayName("書籍を正常に作成できることをテストする")
    fun testCreateBook() {
        // Given: 書籍作成用の引数を準備
        val title = "Spring Boot入門"
        val price = 3000
        val authorIds = listOf(testAuthor.id!!)
        val publicationStatus = "PUBLISHED"

        // When: 書籍を作成
        val result = bookRepository.create(title, price, authorIds, publicationStatus)

        // Then: 書籍が正常に作成されたことを検証
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
        // Given: 複数の著者を持つ書籍作成用の引数
        val title = "Kotlin完全ガイド"
        val price = 4000
        val authorIds = listOf(testAuthor.id!!, testAuthor2.id!!)
        val publicationStatus = "PUBLISHED"

        // When: 書籍を作成
        val result = bookRepository.create(title, price, authorIds, publicationStatus)

        // Then: 複数の著者を持つ書籍が正常に作成されたことを検証
        assertNotNull(result.id)
        assertEquals(title, result.title)
        assertEquals(2, result.authors.size)
        assertTrue(result.authors.map { it.id }.contains(testAuthor.id))
        assertTrue(result.authors.map { it.id }.contains(testAuthor2.id))
    }

    @Test
    @DisplayName("IDで書籍を検索できることをテストする")
    fun testFindBookById() {
        // Given: 書籍を作成
        val createdBook = bookRepository.create(
            "Kotlin入門",
            2000,
            listOf(testAuthor.id!!),
            "PUBLISHED"
        )
        val bookId = createdBook.id!!

        // When: IDで書籍を検索
        val result = bookRepository.findById(bookId)

        // Then: 書籍が正常に検索されたことを検証
        assertNotNull(result)
        assertEquals(bookId, result?.id)
        assertEquals("Kotlin入門", result?.title)
        assertEquals(2000, result?.price)
    }

    @Test
    @DisplayName("存在しない書籍をIDで検索するとnullが返されることをテストする")
    fun testFindNonExistentBookById() {
        // Given: 存在しないID
        val nonExistentId = 99999

        // When: 存在しない書籍を検索
        val result = bookRepository.findById(nonExistentId)

        // Then: nullが返されることを検証
        assertNull(result)
    }

    @Test
    @DisplayName("著者IDで書籍を検索できることをテストする")
    fun testFindBooksByAuthorId() {
        // Given: テスト著者で複数の書籍を作成
        val book1 = bookRepository.create("本1", 1000, listOf(testAuthor.id!!), "PUBLISHED")
        val book2 = bookRepository.create("本2", 2000, listOf(testAuthor.id!!), "UNPUBLISHED")
        bookRepository.create("本3", 3000, listOf(testAuthor2.id!!), "PUBLISHED")

        // When: テスト著者で書籍を検索
        val result = bookRepository.findByAuthorId(testAuthor.id!!)

        // Then: テスト著者の書籍のみが返されることを検証
        assertEquals(2, result.size)
        assertTrue(result.map { it.id }.contains(book1.id))
        assertTrue(result.map { it.id }.contains(book2.id))
    }

    @Test
    @DisplayName("複数の著者を持つ書籍を著者IDで検索できることをテストする")
    fun testFindBooksWithMultipleAuthorsByAuthorId() {
        // Given: 複数の著者を持つ書籍を作成
        val book = bookRepository.create(
            "共著本",
            3000,
            listOf(testAuthor.id!!, testAuthor2.id!!),
            "PUBLISHED"
        )

        // When: いずれかの著者IDで検索
        val resultByAuthor1 = bookRepository.findByAuthorId(testAuthor.id!!)
        val resultByAuthor2 = bookRepository.findByAuthorId(testAuthor2.id!!)

        // Then: 両方の著者IDでその書籍が返されることを検証
        assertTrue(resultByAuthor1.map { it.id }.contains(book.id))
        assertTrue(resultByAuthor2.map { it.id }.contains(book.id))
    }

    @Test
    @DisplayName("書籍情報を正常に更新できることをテストする")
    fun testUpdateBook() {
        // Given: 書籍を作成してから更新する
        val book = bookRepository.create(
            "元のタイトル",
            1000,
            listOf(testAuthor.id!!),
            "UNPUBLISHED"
        )
        val bookId = book.id!!

        // When: 書籍情報を更新
        val result = bookRepository.update(
            bookId,
            "新しいタイトル",
            2000,
            "PUBLISHED",
            listOf(testAuthor.id!!)
        )

        // Then: 書籍情報が正常に更新されたことを検証
        assertNotNull(result)
        assertEquals(bookId, result?.id)
        assertEquals("新しいタイトル", result?.title)
        assertEquals(2000, result?.price)
        assertEquals("PUBLISHED", result?.publicationStatus)
    }

    @Test
    @DisplayName("書籍の著者を変更できることをテストする")
    fun testUpdateBookAuthors() {
        // Given: 書籍を作成してから著者を変更
        val book = bookRepository.create(
            "本",
            1000,
            listOf(testAuthor.id!!),
            "PUBLISHED"
        )
        val bookId = book.id!!

        // When: 著者を変更
        val result = bookRepository.update(
            bookId,
            book.title,
            book.price,
            book.publicationStatus,
            listOf(testAuthor2.id!!)
        )

        // Then: 著者が変更されたことを検証
        assertNotNull(result)
        assertEquals(1, result?.authors?.size)
        assertEquals(testAuthor2.id, result?.authors?.get(0)?.id)
    }

    @Test
    @DisplayName("複数の著者を1人に減らせることをテストする")
    fun testUpdateBookRemoveAuthor() {
        // Given: 複数の著者を持つ書籍を作成
        val book = bookRepository.create(
            "共著本",
            3000,
            listOf(testAuthor.id!!, testAuthor2.id!!),
            "PUBLISHED"
        )
        val bookId = book.id!!

        // When: 著者を1人に減らす
        val result = bookRepository.update(
            bookId,
            book.title,
            book.price,
            book.publicationStatus,
            listOf(testAuthor.id!!)
        )

        // Then: 著者が1人になったことを検証
        assertNotNull(result)
        assertEquals(1, result?.authors?.size)
        assertEquals(testAuthor.id, result?.authors?.get(0)?.id)
    }

    @Test
    @DisplayName("1人の著者を複数人に増やせることをテストする")
    fun testUpdateBookAddAuthor() {
        // Given: 1人の著者を持つ書籍を作成
        val book = bookRepository.create(
            "本",
            1000,
            listOf(testAuthor.id!!),
            "PUBLISHED"
        )
        val bookId = book.id!!

        // When: 著者を追加
        val result = bookRepository.update(
            bookId,
            book.title,
            book.price,
            book.publicationStatus,
            listOf(testAuthor.id!!, testAuthor2.id!!)
        )

        // Then: 著者が増えたことを検証
        assertNotNull(result)
        assertEquals(2, result?.authors?.size)
    }

    @Test
    @DisplayName("存在しない書籍を更新しようとするとnullが返されることをテストする")
    fun testUpdateNonExistentBook() {
        // Given: 存在しないID
        val nonExistentId = 99999

        // When: 存在しない書籍を更新しようとする
        val result = bookRepository.update(
            nonExistentId,
            "タイトル",
            1000,
            "PUBLISHED",
            listOf(testAuthor.id!!)
        )

        // Then: nullが返されることを検証
        assertNull(result)
    }

    @Test
    @DisplayName("複数の書籍を作成・検索できることをテストする")
    fun testCreateAndFindMultipleBooks() {
        // Given: 複数の書籍を作成
        val book1 = bookRepository.create("本1", 1000, listOf(testAuthor.id!!), "PUBLISHED")
        val book2 = bookRepository.create("本2", 2000, listOf(testAuthor.id!!), "UNPUBLISHED")

        // When: それぞれをIDで検索
        val foundBook1 = bookRepository.findById(book1.id!!)
        val foundBook2 = bookRepository.findById(book2.id!!)

        // Then: 両方の書籍が正常に検索されたことを検証
        assertNotNull(foundBook1)
        assertNotNull(foundBook2)
        assertEquals("本1", foundBook1?.title)
        assertEquals("本2", foundBook2?.title)
    }
}

