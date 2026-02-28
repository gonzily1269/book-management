package io.github.gonzily1269.book_management.repository

import io.github.gonzily1269.book_management.dto.AuthorDto
import io.github.gonzily1269.book_management.dto.BookDto
import org.jooq.DSLContext
import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.table
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DataAccessResourceFailureException
import org.springframework.stereotype.Repository
import java.time.LocalDate

/**
 * 書籍リポジトリ
 *
 * データベースから書籍情報を取得・操作するためのリポジトリクラス。
 * jOOQを使用してSQLを実行し、書籍と著者の関連情報を管理する。
 *
 * @property dsl jOOQのDSLコンテキスト
 * @property createFailedMessage 書籍作成失敗時のエラーメッセージ
 * @property retrieveFailedMessage 書籍取得失敗時のエラーメッセージ
 */
@Repository
class BookRepository(
    private val dsl: DSLContext,

    @Value("\${error.repository.book.create-failed}")
    private val createFailedMessage: String,

    @Value("\${error.repository.book.retrieve-failed}")
    private val retrieveFailedMessage: String
) {

    /**
     * IDで書籍を検索
     *
     * @param id 書籍ID
     * @return 見つかった場合は書籍DTO、見つからない場合はnull
     */
    fun findById(id: Int): BookDto? {
        return dsl.select()
            .from("book")
            .where(field("id").eq(id))
            .fetchOne { record ->
                BookDto(
                    id = record.get("id", Int::class.java),
                    title = record.get("title", String::class.java) ?: "",
                    price = record.get("price", Int::class.java) ?: 0,
                    publicationStatus = record.get("publication_status", String::class.java) ?: ""
                )
            }?.let { book ->
                book.id?.let { bookId ->
                    book.copy(authors = findAuthorsByBookId(bookId))
                }
            }
    }

    /**
     * 著者IDで書籍を検索
     *
     * @param authorId 著者ID
     * @return 指定された著者が執筆した書籍のリスト
     */
    fun findByAuthorId(authorId: Int): List<BookDto> {
        return dsl.select(
            field("book.id"),
            field("book.title"),
            field("book.price"),
            field("book.publication_status")
        )
            .from("book")
            .join("book_author").on(field("book.id").eq(field("book_author.book_id")))
            .where(field("book_author.author_id").eq(authorId))
            .fetch { record ->
                BookDto(
                    id = record.get(field("book.id"), Int::class.java),
                    title = record.get(field("book.title"), String::class.java),
                    price = record.get(field("book.price"), Int::class.java),
                    publicationStatus = record.get(field("book.publication_status"), String::class.java)
                )
            }
            .mapNotNull { book ->
                book.id?.let { id ->
                    book.copy(authors = findAuthorsByBookId(id))
                }
            }
    }

    /**
     * 新しい書籍を作成
     *
     * @param title 書籍タイトル
     * @param price 書籍価格
     * @param authorIds 著者IDリスト
     * @param publicationStatus 出版状態
     * @return 作成された書籍DTO
     * @throws IllegalStateException 書籍の作成または取得に失敗した場合
     */
    fun create(title: String, price: Int, authorIds: List<Int>, publicationStatus: String): BookDto {
        val bookId = dsl.insertInto(table("book"))
            .columns(field("title"), field("price"), field("publication_status"))
            .values(title, price, publicationStatus)
            .returningResult(field("id"))
            .fetchOne()
            ?.get("id", Int::class.java)
            ?: throw DataAccessResourceFailureException(createFailedMessage)

        // 著者との関連付け
        authorIds.forEach { authorId ->
            dsl.insertInto(table("book_author"))
                .columns(field("book_id"), field("author_id"))
                .values(bookId, authorId)
                .execute()
        }

        return BookDto(
            id = bookId,
            title = title,
            price = price,
            publicationStatus = publicationStatus,
            authors = findAuthorsByBookId(bookId)
        )
    }

    /**
     * 書籍情報を更新
     *
     * 書籍情報と著者関連付けを更新する。
     *
     * @param id 書籍ID
     * @param title 更新する書籍タイトル
     * @param price 更新する書籍価格
     * @param publicationStatus 更新する出版状態
     * @param authorIds 更新する著者IDリスト
     * @return 更新された書籍DTO、見つからない場合はnull
     */
    fun update(id: Int, title: String, price: Int, publicationStatus: String, authorIds: List<Int>): BookDto? {
        // 書籍情報の更新
        val book = dsl.update(table("book"))
            .set(field("title"), title)
            .set(field("price"), price)
            .set(field("publication_status"), publicationStatus)
            .where(field("id").eq(id))
            .returningResult(field("id"), field("title"), field("price"), field("publication_status"))
            .fetchOne { record ->
                BookDto(
                    id = record.get(field("id"), Int::class.java),
                    title = record.get(field("title"), String::class.java),
                    price = record.get(field("price"), Int::class.java),
                    publicationStatus = record.get(field("publication_status"), String::class.java)
                )
            } ?: return null

        // 著者関連付けの差分更新
        val currentAuthorIds = dsl.select(field("author_id"))
            .from("book_author")
            .where(field("book_id").eq(id))
            .fetch(field("author_id"), Int::class.java)
            .toSet()

        // 差分を計算するために新しい著者IDをセットに変換
        val newAuthorIds = authorIds.toSet()

        // 削除が必要な著者
        (currentAuthorIds - newAuthorIds).forEach { authorId ->
            dsl.deleteFrom(table("book_author"))
                .where(field("book_id").eq(id).and(field("author_id").eq(authorId)))
                .execute()
        }

        // 追加が必要な著者
        (newAuthorIds - currentAuthorIds).forEach { authorId ->
            dsl.insertInto(table("book_author"))
                .columns(field("book_id"), field("author_id"))
                .values(id, authorId)
                .execute()
        }

        return book.copy(authors = findAuthorsByBookId(id))
    }

    /**
     * 書籍IDに関連付けられた著者リストを取得
     *
     * @param bookId 書籍ID
     * @return 著者DTOのリスト
     */
    private fun findAuthorsByBookId(bookId: Int): List<AuthorDto> {
        return dsl.select(
            field("author.id"),
            field("author.name"),
            field("author.birth_date")
        )
            .from("author")
            .join("book_author").on(field("author.id").eq(field("book_author.author_id")))
            .where(field("book_author.book_id").eq(bookId))
            .fetch { record ->
                AuthorDto(
                    id = record.get(field("author.id"), Int::class.java),
                    name = record.get(field("author.name"), String::class.java) ?: "",
                    birthDate = record.get(field("author.birth_date"), LocalDate::class.java) ?: LocalDate.now()
                )
            }
    }
}
