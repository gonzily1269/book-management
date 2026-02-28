package io.github.gonzily1269.book_management.controller

import io.github.gonzily1269.book_management.dto.BookCreateRequest
import io.github.gonzily1269.book_management.dto.BookDto
import io.github.gonzily1269.book_management.dto.BookUpdateRequest
import io.github.gonzily1269.book_management.service.BookService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 書籍コントローラ
 *
 * 書籍に関するREST APIエンドポイントを提供するコントローラクラス。
 * 書籍の作成、更新、著者IDによる検索機能を提供する。
 *
 * @property bookService 書籍サービス
 */
@RestController
@RequestMapping("/api/books")
class BookController(private val bookService: BookService) {

    /**
     * 書籍を作成
     *
     * @param request 書籍作成リクエスト（バリデーション済み）
     * @return 作成された書籍DTO（ステータスコード201）
     */
    @PostMapping
    fun createBook(@Valid @RequestBody request: BookCreateRequest): ResponseEntity<BookDto> {
        val book = bookService.createBook(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(book)
    }

    /**
     * 書籍を更新
     *
     * @param id 書籍ID
     * @param request 書籍更新リクエスト（バリデーション済み）
     * @return 更新された書籍DTO（ステータスコード200）、見つからない場合は404
     */
    @PutMapping("/{id}")
    fun updateBook(
        @PathVariable id: Int,
        @Valid @RequestBody request: BookUpdateRequest
    ): ResponseEntity<BookDto> {
        return bookService.updateBook(id, request)
            ?.let { book -> ResponseEntity.ok(book) }
            ?: ResponseEntity.notFound().build()
    }

    /**
     * 著者IDで書籍を検索
     *
     * @param authorId 著者ID
     * @return 指定された著者が執筆した書籍のリスト
     */
    @GetMapping("/by-author/{authorId}")
    fun getBooksByAuthorId(@PathVariable authorId: Int): ResponseEntity<List<BookDto>> {
        val books = bookService.getBooksByAuthorId(authorId)
        return ResponseEntity.ok(books)
    }
}
