package io.github.gonzily1269.book_management.exception

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.core.MethodParameter
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
import java.util.stream.Stream
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

    companion object {
        @JvmStatic
        fun invalidValidationInputs(): Stream<Arguments> = Stream.of(
            Arguments.of("太郎", LocalDate.now().plusYears(1).toString()),
            Arguments.of("   ", "1990-01-15"),
            Arguments.of("太郎", null)
        )
    }

    private val handler = GlobalExceptionHandler()
    private val authorEndpoint = "/api/authors"

    private fun request(path: String) = ServletWebRequest(MockHttpServletRequest("POST", path))

    private fun buildAuthorJson(name: String, birthDate: String?) =
        """
            {
                "name": "$name",
                "birthDate": ${birthDate?.let { "\"$it\"" } ?: "null"}
            }
        """.trimIndent()

    @Test
    @DisplayName("バリデーションエラーが400ステータスで返されることをテストする")
    fun testValidationErrorHandling() {
        mockMvc.perform(
            post(authorEndpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .content(buildAuthorJson("", "1990-01-15"))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("バリデーションエラー"))
            .andExpect(jsonPath("$.message").value("リクエストボディのバリデーションに失敗しました"))
            .andExpect(jsonPath("$.errors").exists())
            .andExpect(jsonPath("$.path").value(authorEndpoint))
    }

    @ParameterizedTest(name = "name={0}, birthDate={1}")
    @MethodSource("invalidValidationInputs")
    @DisplayName("入力値バリエーションでバリデーション失敗をテストする")
    fun testValidationErrorCases(name: String, birthDate: String?) {
        mockMvc.perform(
            post(authorEndpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .content(buildAuthorJson(name, birthDate))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("バリデーションエラー"))
    }

    @Test
    @DisplayName("無効な日付形式でのバリデーション失敗をテストする")
    fun testInvalidDateFormatValidationError() {
        mockMvc.perform(
            post(authorEndpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .content(buildAuthorJson("太郎", "invalid-date"))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("バリデーションエラー"))
    }

    @Test
    @DisplayName("エラーレスポンスが正しいJSON構造を持つことをテストする")
    fun testErrorResponseStructure() {
        mockMvc.perform(
            post(authorEndpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .content(buildAuthorJson("", "1990-01-15"))
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
        val responseWithMessage = handler.handleGeneralException(Exception("想定外エラー"), request(authorEndpoint))
        val responseWithoutMessage = handler.handleGeneralException(Exception(), request(authorEndpoint))

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
        val response = handler.handleValidationException(ex, request(authorEndpoint))

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