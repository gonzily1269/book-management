package io.github.gonzily1269.book_management.exception

import org.springframework.dao.DataAccessException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import java.time.LocalDateTime

/**
 * グローバル例外ハンドラ
 *
 * アプリケーション全体で発生する例外を捕捉し、適切なHTTPレスポンスを返すクラス。
 * バリデーションエラー、業務ロジックエラー、予期しないエラーをハンドリングする。
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    /**
     * バリデーションエラーをハンドリング
     *
     * @param ex バリデーション例外
     * @param request Webリクエスト
     * @return エラーレスポンス（ステータスコード400）
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(
        ex: MethodArgumentNotValidException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val errors = mutableMapOf<String, String>()
        ex.bindingResult.fieldErrors.forEach { error ->
            val fieldName = error.field
            val message = error.defaultMessage ?: "バリデーションエラーが発生しました"
            errors[fieldName] = message
        }

        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.BAD_REQUEST.value(),
            error = "バリデーションエラー",
            message = "リクエストボディのバリデーションに失敗しました",
            errors = errors,
            path = request.getDescription(false).removePrefix("uri=")
        )

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
    }

    /**
     * 業務ロジックエラーをハンドリング
     *
     * @param ex 状態不正例外
     * @param request Webリクエスト
     * @return エラーレスポンス（ステータスコード400）
     */
    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalStateException(
        ex: IllegalStateException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.BAD_REQUEST.value(),
            error = "業務ロジックエラー",
            message = ex.message ?: "リクエストが業務ルールに違反しています",
            path = request.getDescription(false).removePrefix("uri=")
        )

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
    }

    /**
     * データベースアクセスエラーをハンドリング
     *
     * @param ex データアクセス例外
     * @param request Webリクエスト
     * @return エラーレスポンス（ステータスコード500）
     */
    @ExceptionHandler(DataAccessException::class)
    fun handleDataAccessException(
        ex: DataAccessException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = "データベースエラー",
            message = ex.message ?: "データベースの操作中にエラーが発生しました",
            path = request.getDescription(false).removePrefix("uri=")
        )

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
    }

    /**
     * その他の予期しないエラーをハンドリング
     *
     * @param ex 発生した例外
     * @param request Webリクエスト
     * @return エラーレスポンス（ステータスコード500）
     */
    @ExceptionHandler(Exception::class)
    fun handleGeneralException(
        ex: Exception,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = "内部サーバーエラー",
            message = ex.message ?: "予期しないエラーが発生しました",
            path = request.getDescription(false).removePrefix("uri=")
        )

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
    }
}

/**
 * エラーレスポンスのデータクラス
 *
 * APIエラー発生時にクライアントに返されるレスポンスボディの構造を定義する。
 *
 * @property timestamp エラー発生日時
 * @property status HTTPステータスコード
 * @property error エラータイプ
 * @property message エラーメッセージ
 * @property path リクエストパス（任意）
 * @property errors フィールドごとのバリデーションエラー（任意）
 */
data class ErrorResponse(
    val timestamp: LocalDateTime,
    val status: Int,
    val error: String,
    val message: String,
    val path: String? = null,
    val errors: Map<String, String> = emptyMap()
)
