package io.github.gonzily1269.book_management.controller

import io.github.gonzily1269.book_management.dto.AuthorCreateRequest
import io.github.gonzily1269.book_management.dto.BookDto
import io.github.gonzily1269.book_management.dto.BookUpdateRequest
import io.github.gonzily1269.book_management.dto.PublicationStatus
import io.github.gonzily1269.book_management.service.AuthorService
import io.github.gonzily1269.book_management.service.BookService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import kotlin.test.assertEquals

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("BookControllerのテスト")
class BookControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val authorService: AuthorService
) {

    private var testAuthorId = 0
    private var testAuthorId2 = 0
    private val booksEndpoint = "/api/books"
    private val byAuthorEndpoint = "/api/books/by-author"

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
    @DisplayName("POST /api/books で書籍を作成できることをテストする")
    fun testCreateBook() {
        createBookRequest()
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").isNumber)
            .andExpect(jsonPath("$.title").value("Spring Boot入門"))
            .andExpect(jsonPath("$.price").value(3000))
    }

    @Test
    @DisplayName("POST /api/books で複数の著者を持つ書籍を作成できることをテストする")
    fun testCreateBookWithMultipleAuthors() {
        createBookRequest(
            title = "Kotlin完全ガイド",
            price = 4000,
            authorIds = listOf(testAuthorId, testAuthorId2)
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.authors.length()").value(2))
    }

    @Test
    @DisplayName("POST /api/books で空のタイトルではバリデーションエラーになることをテストする")
    fun testCreateBookWithEmptyTitle() {
        createBookRequest(title = "")
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("バリデーションエラー"))
    }

    @Test
    @DisplayName("PUT /api/books/{id} で書籍を更新できることをテストする")
    fun testUpdateBook() {
        val createResponse = createBook("元のタイトル", 1000, listOf(testAuthorId), "UNPUBLISHED")
            .response
            .contentAsString

        val bookId = extractId(createResponse)

        updateBookRequest(bookId)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.title").value("新しいタイトル"))
            .andExpect(jsonPath("$.price").value(2000))
    }

    @Test
    @DisplayName("PUT /api/books/{id} で出版済みから未出版に変更するとエラーになることをテストする")
    fun testCannotUnpublishPublishedBook() {
        val createResponse = createBook("出版済み本", 3000, listOf(testAuthorId), "PUBLISHED")
            .response
            .contentAsString

        val bookId = extractId(createResponse)

        updateBookRequest(
            bookId = bookId,
            title = "出版済み本",
            price = 3000,
            publicationStatus = "UNPUBLISHED"
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("業務ロジックエラー"))
    }

    @Test
    @DisplayName("PUT /api/books/{id} で存在しない書籍を更新しようとすると404が返されることをテストする")
    fun testUpdateNonExistentBook() {
        val nonExistentId = 99999

        updateBookRequest(
            bookId = nonExistentId,
            title = "更新タイトル",
            price = 2000,
            publicationStatus = "PUBLISHED"
        )
            .andExpect(status().isNotFound)
    }

    @Test
    @DisplayName("GET /api/books/by-author/{authorId} で著者の書籍を検索できることをテストする")
    fun testGetBooksByAuthorId() {
        createBook("本1", 1000, listOf(testAuthorId), "PUBLISHED")
        createBook("本2", 2000, listOf(testAuthorId), "UNPUBLISHED")

        mockMvc.perform(get("$byAuthorEndpoint/$testAuthorId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
    }

    @Test
    @DisplayName("POST /api/books で存在しない著者IDを指定すると業務ロジックエラーになることをテストする")
    fun testCreateBookWithInvalidAuthorId() {
        createBookRequest(authorIds = listOf(99999))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("業務ロジックエラー"))
    }

    @Test
    @DisplayName("POST /api/books で重複した著者IDを指定すると業務ロジックエラーになることをテストする")
    fun testCreateBookWithDuplicateAuthorIds() {
        createBookRequest(authorIds = listOf(testAuthorId, testAuthorId))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("業務ロジックエラー"))
    }

    @Test
    @DisplayName("updateBookの分岐を直接呼び出しで網羅できることをテストする（200/404）")
    fun testUpdateBookBranchByDirectInvocation() {
        val service = Mockito.mock(BookService::class.java)
        val controller = BookController(service)

        val request = BookUpdateRequest(
            title = "t",
            price = 100,
            publicationStatus = PublicationStatus.PUBLISHED,
            authorIds = listOf(testAuthorId)
        )

        val dto = BookDto(id = 1, title = "t", price = 100, publicationStatus = PublicationStatus.PUBLISHED)

        Mockito.`when`(service.updateBook(1, request)).thenReturn(dto)
        Mockito.`when`(service.updateBook(999, request)).thenReturn(null)

        val okResponse = controller.updateBook(1, request)
        val notFoundResponse = controller.updateBook(999, request)

        assertEquals(200, okResponse.statusCode.value())
        assertEquals(404, notFoundResponse.statusCode.value())
    }

    private fun extractId(json: String): Int {
        return """"id":\s*(\d+)""".toRegex().find(json)?.groupValues?.get(1)?.toInt()
            ?: throw IllegalStateException("IDが見つかりません: $json")
    }

    private fun buildBookJson(
        title: String,
        price: Int,
        authorIds: List<Int>,
        publicationStatus: String
    ) =
        """
            {
                "title": "$title",
                "price": $price,
                "authorIds": ${authorIds.joinToString(prefix = "[", postfix = "]")},
                "publicationStatus": "$publicationStatus"
            }
        """.trimIndent()

    private fun createBookRequest(
        title: String = "Spring Boot入門",
        price: Int = 3000,
        authorIds: List<Int> = listOf(testAuthorId),
        publicationStatus: String = "PUBLISHED"
    ): ResultActions =
        mockMvc.perform(
            post(booksEndpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .content(buildBookJson(title, price, authorIds, publicationStatus))
        )

    private fun createBook(
        title: String = "Spring Boot入門",
        price: Int = 3000,
        authorIds: List<Int> = listOf(testAuthorId),
        publicationStatus: String = "PUBLISHED"
    ): MvcResult =
        createBookRequest(title, price, authorIds, publicationStatus).andReturn()

    private fun updateBookRequest(
        bookId: Int,
        title: String = "新しいタイトル",
        price: Int = 2000,
        authorIds: List<Int> = listOf(testAuthorId),
        publicationStatus: String = "UNPUBLISHED"
    ): ResultActions =
        mockMvc.perform(
            put("$booksEndpoint/$bookId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(buildBookJson(title, price, authorIds, publicationStatus))
        )
}