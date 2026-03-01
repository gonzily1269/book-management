package io.github.gonzily1269.book_management.service

import io.github.gonzily1269.book_management.dto.BookCreateRequest
import io.github.gonzily1269.book_management.dto.BookDto
import io.github.gonzily1269.book_management.dto.BookUpdateRequest
import io.github.gonzily1269.book_management.dto.PublicationStatus
import io.github.gonzily1269.book_management.repository.AuthorRepository
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
    private val authorRepository: AuthorRepository,

    @Value("\${error.book.published-cannot-unpublish}")
    private val publishedCannotUnpublish: String,

    @Value("\${error.book.duplicate-author-ids}")
    private val duplicateAuthorIds: String,

    @Value("\${error.book.author-not-found}")
    private val authorNotFound: String
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
        validateUniqueAuthorIds(request.authorIds)
        validateAuthorIds(request.authorIds)

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

        // 仕様上、出版済みから未出版への巻き戻しは許可しない
        if (currentBook.publicationStatus == PublicationStatus.PUBLISHED && request.publicationStatus == PublicationStatus.UNPUBLISHED) {
            throw IllegalStateException(publishedCannotUnpublish)
        }

        validateUniqueAuthorIds(request.authorIds)
        validateAuthorIds(request.authorIds)

        return bookRepository.update(
            id,
            request.title,
            request.price,
            request.publicationStatus,
            request.authorIds
        )
    }

    /**
     * 著者IDの存在を事前検証し、FK違反をDB例外ではなく業務エラーとして扱う。
     */
    private fun validateAuthorIds(authorIds: List<Int>) {
        if (!authorRepository.existsAllByIds(authorIds)) {
            throw IllegalStateException(authorNotFound)
        }
    }

    /**
     * 著者IDの重複を事前に検知する。
     *
     * 重複を許可すると book_author への登録時に複合主キー違反へ到達するため、
     * DB例外に依存せず業務エラーとして返す。
     */
    private fun validateUniqueAuthorIds(authorIds: List<Int>) {
        if (authorIds.toSet().size != authorIds.size) {
            throw IllegalStateException(duplicateAuthorIds)
        }
    }
}

