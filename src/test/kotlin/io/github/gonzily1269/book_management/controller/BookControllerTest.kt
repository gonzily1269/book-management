package io.github.gonzily1269.book_management.controller

import io.github.gonzily1269.book_management.dto.AuthorCreateRequest
import io.github.gonzily1269.book_management.dto.BookDto
import io.github.gonzily1269.book_management.dto.BookUpdateRequest
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

    // ID抽出用の共通関数
    private fun extractId(json: String): Int {
        return """"id":\s*(\d+)""".toRegex().find(json)?.groupValues?.get(1)?.toInt()
            ?: throw IllegalStateException("IDが見つかりません: $json")
    }

    @Test
    @DisplayName("POST /api/books で書籍を作成できることをテストする")
    fun testCreateBook() {
        val requestJson = """
            {
                "title": "Spring Boot入門",
                "price": 3000,
                "authorIds": [$testAuthorId],
                "publicationStatus": "PUBLISHED"
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").isNumber)
            .andExpect(jsonPath("$.title").value("Spring Boot入門"))
            .andExpect(jsonPath("$.price").value(3000))
    }

    @Test
    @DisplayName("POST /api/books で複数の著者を持つ書籍を作成できることをテストする")
    fun testCreateBookWithMultipleAuthors() {
        val requestJson = """
            {
                "title": "Kotlin完全ガイド",
                "price": 4000,
                "authorIds": [$testAuthorId, $testAuthorId2],
                "publicationStatus": "PUBLISHED"
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.authors.length()").value(2))
    }

    @Test
    @DisplayName("POST /api/books で空のタイトルではバリデーションエラーになることをテストする")
    fun testCreateBookWithEmptyTitle() {
        val requestJson = """
            {
                "title": "",
                "price": 3000,
                "authorIds": [$testAuthorId],
                "publicationStatus": "PUBLISHED"
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("バリデーションエラー"))
    }

    @Test
    @DisplayName("PUT /api/books/{id} で書籍を更新できることをテストする")
    fun testUpdateBook() {
        val createJson =
            """{"title": "元のタイトル", "price": 1000, "authorIds": [$testAuthorId], "publicationStatus": "UNPUBLISHED"}"""
        val createResponse = mockMvc.perform(
            post("/api/books").contentType(MediaType.APPLICATION_JSON).content(createJson)
        ).andReturn().response.contentAsString

        val bookId = extractId(createResponse)

        val updateJson = """
            {
                "title": "新しいタイトル",
                "price": 2000,
                "authorIds": [$testAuthorId],
                "publicationStatus": "UNPUBLISHED"
            }
        """.trimIndent()

        mockMvc.perform(
            put("/api/books/$bookId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.title").value("新しいタイトル"))
            .andExpect(jsonPath("$.price").value(2000))
    }

    @Test
    @DisplayName("PUT /api/books/{id} で出版済みから未出版に変更するとエラーになることをテストする")
    fun testCannotUnpublishPublishedBook() {
        val createJson =
            """{"title": "出版済み本", "price": 3000, "authorIds": [$testAuthorId], "publicationStatus": "PUBLISHED"}"""
        val createResponse = mockMvc.perform(
            post("/api/books").contentType(MediaType.APPLICATION_JSON).content(createJson)
        ).andReturn().response.contentAsString

        val bookId = extractId(createResponse)

        val updateJson =
            """{"title": "出版済み本", "price": 3000, "authorIds": [$testAuthorId], "publicationStatus": "UNPUBLISHED"}"""

        mockMvc.perform(
            put("/api/books/$bookId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("業務ロジックエラー"))
    }

    @Test
    @DisplayName("PUT /api/books/{id} で存在しない書籍を更新しようとすると404が返されることをテストする")
    fun testUpdateNonExistentBook() {
        val nonExistentId = 99999
        val updateJson = """
            {
                "title": "更新タイトル",
                "price": 2000,
                "authorIds": [$testAuthorId],
                "publicationStatus": "PUBLISHED"
            }
        """.trimIndent()

        mockMvc.perform(
            put("/api/books/$nonExistentId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson)
        )
            .andExpect(status().isNotFound)
    }

    @Test
    @DisplayName("GET /api/books/by-author/{authorId} で著者の書籍を検索できることをテストする")
    fun testGetBooksByAuthorId() {
        val book1 =
            """{"title": "本1", "price": 1000, "authorIds": [$testAuthorId], "publicationStatus": "PUBLISHED"}"""
        val book2 =
            """{"title": "本2", "price": 2000, "authorIds": [$testAuthorId], "publicationStatus": "UNPUBLISHED"}"""

        mockMvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON).content(book1))
        mockMvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON).content(book2))

        mockMvc.perform(get("/api/books/by-author/$testAuthorId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
    }

    @Test
    @DisplayName("updateBookの分岐を直接呼び出しで網羅する（200/404）")
    fun testUpdateBookBranchByDirectInvocation() {
        val service = Mockito.mock(BookService::class.java)
        val controller = BookController(service)

        val request = BookUpdateRequest(
            title = "t",
            price = 100,
            publicationStatus = "PUBLISHED",
            authorIds = listOf(testAuthorId)
        )

        val dto = BookDto(id = 1, title = "t", price = 100, publicationStatus = "PUBLISHED")

        Mockito.`when`(service.updateBook(1, request)).thenReturn(dto)
        Mockito.`when`(service.updateBook(999, request)).thenReturn(null)

        val okResponse = controller.updateBook(1, request)
        val notFoundResponse = controller.updateBook(999, request)

        assertEquals(200, okResponse.statusCode.value())
        assertEquals(404, notFoundResponse.statusCode.value())
    }
}