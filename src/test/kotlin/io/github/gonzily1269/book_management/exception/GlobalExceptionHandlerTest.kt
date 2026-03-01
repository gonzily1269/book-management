package io.github.gonzily1269.book_management.exception

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.core.MethodParameter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.context.request.ServletWebRequest
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * GlobalExceptionHandlerのテストクラス
 *
 * グローバル例外ハンドラが各種例外を正しくハンドリングすることをテストする。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("GlobalExceptionHandlerのテスト")
class GlobalExceptionHandlerTest @Autowired constructor(
    private val mockMvc: MockMvc
) {

    private val handler = GlobalExceptionHandler()

    private fun request(path: String) = ServletWebRequest(MockHttpServletRequest("POST", path))

    @Test
    @DisplayName("バリデーションエラーが400ステータスで返されることをテストする")
    fun testValidationErrorHandling() {
        // Given: バリデーション規則に違反する著者作成リクエスト (名前が空)
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
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("バリデーションエラー"))
            .andExpect(jsonPath("$.message").value("リクエストボディのバリデーションに失敗しました"))
            .andExpect(jsonPath("$.errors").exists())
            .andExpect(jsonPath("$.path").value("/api/authors"))
    }

    @Test
    @DisplayName("未来の生年月日でのバリデーション失敗をテストする")
    fun testFutureBirthDateValidationError() {
        // Given: 未来の生年月日を動的に生成
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
    @DisplayName("空白のみの名前でのバリデーション失敗をテストする")
    fun testBlankNameValidationError() {
        // Given: 空白のみの名前
        val requestJson = """
            {
                "name": "   ",
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
    @DisplayName("nullの生年月日でのバリデーション失敗をテストする")
    fun testNullBirthDateValidationError() {
        // Given: nullの生年月日を含むリクエスト
        val requestJson = """
            {
                "name": "太郎",
                "birthDate": null
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
    @DisplayName("無効な日付形式でのバリデーション失敗をテストする")
    fun testInvalidDateFormatValidationError() {
        // Given: 無効な日付形式 ("invalid-date")
        val requestJson = """
            {
                "name": "太郎",
                "birthDate": "invalid-date"
            }
        """.trimIndent()

        // When & Then
        mockMvc.perform(
            post("/api/authors")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("エラーレスポンスが正しいJSON構造を持つことをテストする")
    fun testErrorResponseStructure() {
        // Given: バリデーション違反
        val requestJson = """{"name": "", "birthDate": "1990-01-15"}"""

        // When & Then
        mockMvc.perform(
            post("/api/authors")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.timestamp").isString)
            .andExpect(jsonPath("$.status").isNumber)
            .andExpect(jsonPath("$.error").isString)
            .andExpect(jsonPath("$.message").isString)
            .andExpect(jsonPath("$.path").isString)
            .andExpect(jsonPath("$.errors").isMap)
    }

    @Test
    @DisplayName("IllegalStateExceptionのメッセージがnullの場合はデフォルト文言を返す")
    fun testHandleIllegalStateExceptionWithNullMessage() {
        val response = handler.handleIllegalStateException(IllegalStateException(), request("/api/books/1"))

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertNotNull(response.body)
        assertEquals("業務ロジックエラー", response.body!!.error)
        assertEquals("リクエストが業務ルールに違反しています", response.body!!.message)
        assertEquals("/api/books/1", response.body!!.path)
    }

    @Test
    @DisplayName("一般例外のメッセージ有無で分岐を網羅する")
    fun testHandleGeneralExceptionMessageBranches() {
        val responseWithMessage = handler.handleGeneralException(Exception("想定外エラー"), request("/api/authors"))
        val responseWithoutMessage = handler.handleGeneralException(Exception(), request("/api/authors"))

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseWithMessage.statusCode)
        assertEquals("想定外エラー", responseWithMessage.body!!.message)

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseWithoutMessage.statusCode)
        assertEquals("予期しないエラーが発生しました", responseWithoutMessage.body!!.message)
    }

    @Test
    @DisplayName("バリデーションエラーのdefaultMessageがnullの場合の分岐を網羅する")
    fun testHandleValidationExceptionWithNullDefaultMessage() {
        val method = DummyValidationTarget::class.java.getDeclaredMethod("dummy", String::class.java)
        val parameter = MethodParameter(method, 0)

        val bindingResult = BeanPropertyBindingResult(Any(), "dummyTarget")
        bindingResult.addError(
            FieldError(
                "dummyTarget",
                "name",
                "",
                false,
                emptyArray(),
                emptyArray(),
                null
            )
        )

        val ex = MethodArgumentNotValidException(parameter, bindingResult)
        val response = handler.handleValidationException(ex, request("/api/authors"))

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("バリデーションエラー", response.body!!.error)
        assertEquals("バリデーションエラーが発生しました", response.body!!.errors["name"])
    }

    private class DummyValidationTarget {
        @Suppress("UNUSED_PARAMETER")
        fun dummy(name: String) {
        }
    }
}