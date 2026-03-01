package io.github.gonzily1269.book_management.controller

import io.github.gonzily1269.book_management.dto.AuthorDto
import io.github.gonzily1269.book_management.dto.AuthorUpdateRequest
import io.github.gonzily1269.book_management.service.AuthorService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import kotlin.test.assertEquals

/**
 * AuthorControllerのテストクラス
 *
 * 著者コントローラのREST APIエンドポイントをテストする。
 * MockMvcを使用してHTTPリクエストをシミュレートする。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("AuthorControllerのテスト")
class AuthorControllerTest @Autowired constructor(
    private val mockMvc: MockMvc
) {
    @Test
    @DisplayName("POST /api/authors で著者を作成できることをテストする")
    fun testCreateAuthor() {
        // Given: 著者作成リクエスト (JSON 文字列を直接定義)
        val requestJson = """
            {
                "name": "太郎",
                "birthDate": "1990-01-15"
            }
        """.trimIndent()

        // When & Then: POST リクエストを送信して著者を作成
        mockMvc.perform(
            post("/api/authors")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson) // 直接文字列を渡す
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").isNumber)
            .andExpect(jsonPath("$.name").value("太郎"))
            .andExpect(jsonPath("$.birthDate").value("1990-01-15"))
    }

    @Test
    @DisplayName("POST /api/authors で空の名前ではバリデーションエラーになることをテストする")
    fun testCreateAuthorWithEmptyName() {
        // Given: 空の名前を含む JSON
        val requestJson = """
            {
                "name": "",
                "birthDate": "1990-01-15"
            }
        """.trimIndent()

        // When & Then
        mockMvc.perform(
            post("/api/authors")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("バリデーションエラー"))
    }

    @Test
    @DisplayName("POST /api/authors で未来の生年月日ではバリデーションエラーになることをテストする")
    fun testCreateAuthorWithFutureBirthDate() {
        // Given: 未来の日付を動的に生成
        val futureDate = LocalDate.now().plusYears(1)
        val requestJson = """
            {
                "name": "太郎",
                "birthDate": "$futureDate"
            }
        """.trimIndent()

        // When & Then
        mockMvc.perform(
            post("/api/authors")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("バリデーションエラー"))
    }

    @Test
    @DisplayName("PUT /api/authors/{id} で著者を更新できることをテストする")
    fun testUpdateAuthor() {
        // 1. 著者を作成し、レスポンスを文字列として取得
        val createJson = """{"name": "太郎", "birthDate": "1990-01-15"}"""
        val createResponse = mockMvc.perform(
            post("/api/authors")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson)
        ).andReturn().response.contentAsString

        // 正規表現で "id":123 のような数値部分だけを抜き出す
        val idRegex = """"id":\s*(\d+)""".toRegex()
        val authorId = idRegex.find(createResponse)?.groupValues?.get(1)?.toInt()
            ?: throw IllegalStateException("レスポンスからIDを抽出できませんでした: $createResponse")

        // 2. 更新用 JSON (トリプルクォート)
        val updateJson = """
            {
                "name": "次郎",
                "birthDate": "1995-03-10"
            }
        """.trimIndent()

        // When & Then: 抽出した authorId を使って PUT リクエスト
        mockMvc.perform(
            put("/api/authors/$authorId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(authorId))
            .andExpect(jsonPath("$.name").value("次郎"))
            .andExpect(jsonPath("$.birthDate").value("1995-03-10"))
    }

    @Test
    @DisplayName("PUT /api/authors/{id} で存在しない著者を更新しようとすると404が返されることをテストする")
    fun testUpdateNonExistentAuthor() {
        // Given
        val nonExistentId = 99999
        val updateJson = """{"name": "新しい名前", "birthDate": "${LocalDate.now()}"}"""

        // When & Then
        mockMvc.perform(
            put("/api/authors/$nonExistentId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson)
        )
            .andExpect(status().isNotFound)
    }

    @Test
    @DisplayName("PUT /api/authors/{id} で空の名前ではバリデーションエラーになることをテストする")
    fun testUpdateAuthorWithEmptyName() {
        // 1. 著者を作成
        val createJson = """{"name": "太郎", "birthDate": "1990-01-15"}"""
        val createResponse = mockMvc.perform(
            post("/api/authors").contentType(MediaType.APPLICATION_JSON).content(createJson)
        ).andReturn().response.contentAsString

        // IDを抽出
        val authorId = """"id":\s*(\d+)""".toRegex().find(createResponse)?.groupValues?.get(1)

        // 2. 空の名前の更新 JSON
        val updateJson = """{"name": "", "birthDate": "1990-01-15"}"""

        // When & Then
        mockMvc.perform(
            put("/api/authors/$authorId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("バリデーションエラー"))
    }

    @Test
    @DisplayName("複数の著者を作成できることをテストする")
    fun testCreateMultipleAuthors() {
        val request1 = """{"name": "太郎", "birthDate": "1990-01-15"}"""
        val request2 = """{"name": "花子", "birthDate": "1985-06-20"}"""

        mockMvc.perform(post("/api/authors").contentType(MediaType.APPLICATION_JSON).content(request1))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("太郎"))

        mockMvc.perform(post("/api/authors").contentType(MediaType.APPLICATION_JSON).content(request2))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("花子"))
    }

    @Test
    @DisplayName("updateAuthorの分岐を直接呼び出しで網羅する（200/404）")
    fun testUpdateAuthorBranchByDirectInvocation() {
        val service = Mockito.mock(AuthorService::class.java)
        val controller = AuthorController(service)

        val request = AuthorUpdateRequest(
            name = "n",
            birthDate = LocalDate.of(1990, 1, 1)
        )

        val dto = AuthorDto(id = 1, name = "n", birthDate = LocalDate.of(1990, 1, 1))

        Mockito.`when`(service.updateAuthor(1, request)).thenReturn(dto)
        Mockito.`when`(service.updateAuthor(999, request)).thenReturn(null)

        val okResponse = controller.updateAuthor(1, request)
        val notFoundResponse = controller.updateAuthor(999, request)

        assertEquals(200, okResponse.statusCode.value())
        assertEquals(404, notFoundResponse.statusCode.value())
    }
}

