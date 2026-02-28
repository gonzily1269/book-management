package io.github.gonzily1269.book_management.controller

import io.github.gonzily1269.book_management.dto.AuthorCreateRequest
import io.github.gonzily1269.book_management.dto.AuthorDto
import io.github.gonzily1269.book_management.dto.AuthorUpdateRequest
import io.github.gonzily1269.book_management.service.AuthorService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 著者コントローラ
 *
 * 著者に関するREST APIエンドポイントを提供するコントローラクラス。
 * 著者の作成と更新機能を提供する。
 *
 * @property authorService 著者サービス
 */
@RestController
@RequestMapping("/api/authors")
class AuthorController(private val authorService: AuthorService) {

    /**
     * 著者を作成
     *
     * @param request 著者作成リクエスト（バリデーション済み）
     * @return 作成された著者DTO（ステータスコード201）
     */
    @PostMapping
    fun createAuthor(@Valid @RequestBody request: AuthorCreateRequest): ResponseEntity<AuthorDto> {
        val author = authorService.createAuthor(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(author)
    }

    /**
     * 著者を更新
     *
     * @param id 著者ID
     * @param request 著者更新リクエスト（バリデーション済み）
     * @return 更新された著者DTO（ステータスコード200）、見つからない場合は404
     */
    @PutMapping("/{id}")
    fun updateAuthor(
        @PathVariable id: Int,
        @Valid @RequestBody request: AuthorUpdateRequest
    ): ResponseEntity<AuthorDto> {
        return authorService.updateAuthor(id, request)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()
    }
}
