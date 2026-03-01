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
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.ResultActions
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
    private val authorEndpoint = "/api/authors"

    private fun buildAuthorJson(name: String, birthDate: String) =
        """
            {
                "name": "$name",
                "birthDate": "$birthDate"
            }
        """.trimIndent()

    private fun createAuthorRequest(
        name: String = "太郎",
        birthDate: String = "1990-01-15"
    ): ResultActions =
        mockMvc.perform(
            post(authorEndpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .content(buildAuthorJson(name, birthDate))
        )

    private fun createAuthor(name: String = "太郎", birthDate: String = "1990-01-15"): MvcResult =
        createAuthorRequest(name, birthDate).andReturn()

    private fun updateAuthorRequest(
        authorId: Int,
        name: String = "次郎",
        birthDate: String = "1995-03-10"
    ): ResultActions =
        mockMvc.perform(
            put("$authorEndpoint/$authorId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(buildAuthorJson(name, birthDate))
        )

    private fun extractId(responseJson: String): Int =
        """"id":\s*(\d+)""".toRegex().find(responseJson)?.groupValues?.get(1)?.toInt()
            ?: error("レスポンスからIDを抽出できませんでした: $responseJson")

    @Test
    @DisplayName("POST /api/authors で著者を作成できることをテストする")
    fun testCreateAuthor() {
        createAuthorRequest()
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").isNumber)
            .andExpect(jsonPath("$.name").value("太郎"))
            .andExpect(jsonPath("$.birthDate").value("1990-01-15"))
    }

    @Test
    @DisplayName("POST /api/authors で空の名前ではバリデーションエラーになることをテストする")
    fun testCreateAuthorWithEmptyName() {
        createAuthorRequest(name = "")
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("バリデーションエラー"))
    }

    @Test
    @DisplayName("POST /api/authors で未来の生年月日ではバリデーションエラーになることをテストする")
    fun testCreateAuthorWithFutureBirthDate() {
        val futureDate = LocalDate.now().plusYears(1)
        createAuthorRequest(birthDate = futureDate.toString())
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("バリデーションエラー"))
    }

    @Test
    @DisplayName("PUT /api/authors/{id} で著者を更新できることをテストする")
    fun testUpdateAuthor() {
        val createResponse = createAuthor().response.contentAsString
        val authorId = extractId(createResponse)

        updateAuthorRequest(authorId)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(authorId))
            .andExpect(jsonPath("$.name").value("次郎"))
            .andExpect(jsonPath("$.birthDate").value("1995-03-10"))
    }

    @Test
    @DisplayName("PUT /api/authors/{id} で存在しない著者を更新しようとすると404が返されることをテストする")
    fun testUpdateNonExistentAuthor() {
        val nonExistentId = 99999
        updateAuthorRequest(
            authorId = nonExistentId,
            name = "新しい名前",
            birthDate = LocalDate.now().toString()
        )
            .andExpect(status().isNotFound)
    }

    @Test
    @DisplayName("PUT /api/authors/{id} で空の名前ではバリデーションエラーになることをテストする")
    fun testUpdateAuthorWithEmptyName() {
        val createResponse = createAuthor().response.contentAsString
        val authorId = extractId(createResponse)

        updateAuthorRequest(authorId = authorId, name = "", birthDate = "1990-01-15")
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("バリデーションエラー"))
    }

    @Test
    @DisplayName("複数の著者を作成できることをテストする")
    fun testCreateMultipleAuthors() {
        createAuthorRequest()
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("太郎"))

        createAuthorRequest("花子", "1985-06-20")
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

