package io.github.gonzily1269.book_management.service

import io.github.gonzily1269.book_management.dto.BookCreateRequest
import io.github.gonzily1269.book_management.dto.BookDto
import io.github.gonzily1269.book_management.dto.BookUpdateRequest
import io.github.gonzily1269.book_management.repository.BookRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 書籍サービス
 *
 * 書籍に関するビジネスロジックを提供するサービスクラス。
 * トランザクション管理を行い、書籍の作成・更新時の業務ルールを適用する。
 *
 * @property bookRepository 書籍リポジトリ
 * @property publishedCannotUnpublish 出版済みから未出版への変更不可エラーメッセージ
 */
@Service
@Transactional
class BookService(
    private val bookRepository: BookRepository,

    @Value("\${error.book.published-cannot-unpublish}")
    private val publishedCannotUnpublish: String
) {

    /**
     * 著者IDで書籍を検索
     *
     * @param authorId 著者ID
     * @return 指定された著者が執筆した書籍のリスト
     */
    fun getBooksByAuthorId(authorId: Int): List<BookDto> {
        return bookRepository.findByAuthorId(authorId)
    }

    /**
     * 新しい書籍を作成
     *
     * @param request 書籍作成リクエスト
     * @return 作成された書籍DTO
     */
    fun createBook(request: BookCreateRequest): BookDto {
        return bookRepository.create(
            request.title,
            request.price,
            request.authorIds,
            request.publicationStatus,
        )
    }

    /**
     * 書籍情報を更新
     *
     * 出版済みから未出版への変更は許可されない。
     *
     * @param id 書籍ID
     * @param request 書籍更新リクエスト
     * @return 更新された書籍DTO、見つからない場合はnull
     * @throws IllegalStateException 出版済みから未出版への変更を試みた場合
     */
    fun updateBook(id: Int, request: BookUpdateRequest): BookDto? {
        val currentBook = bookRepository.findById(id) ?: return null

        // 出版済みから未出版への変更はできない
        if (currentBook.publicationStatus == "PUBLISHED" && request.publicationStatus == "UNPUBLISHED") {
            throw IllegalStateException(publishedCannotUnpublish)
        }

        return bookRepository.update(
            id,
            request.title,
            request.price,
            request.publicationStatus,
            request.authorIds
        )
    }
}

