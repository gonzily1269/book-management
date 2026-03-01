package io.github.gonzily1269.book_management.repository

import io.github.gonzily1269.book_management.dto.AuthorDto
import io.github.gonzily1269.book_management.dto.BookDto
import io.github.gonzily1269.book_management.dto.PublicationStatus
import io.github.gonzily1269.tables.Author
import io.github.gonzily1269.tables.Book
import io.github.gonzily1269.tables.BookAuthor
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

/**
 * 書籍リポジトリ
 *
 * データベースから書籍情報を取得・操作するためのリポジトリクラス。
 * jOOQを使用してSQLを実行し、書籍と著者の関連情報を管理する。
 *
 * @property dsl jOOQのDSLコンテキスト
 */
@Repository
class BookRepository(
    private val dsl: DSLContext
) {

    /**
     * IDで書籍を検索
     *
     * @param id 書籍ID
     * @return 見つかった場合は書籍DTO、見つからない場合はnull
     */
    fun findById(id: Int): BookDto? {
        return dsl.select()
            .from(Book.BOOK)
            .where(Book.BOOK.ID.eq(id))
            .fetchOptional { record ->
                val bookId = record.get(Book.BOOK.ID)!!
                BookDto(
                    id = bookId,
                    title = record.get(Book.BOOK.TITLE),
                    price = record.get(Book.BOOK.PRICE),
                    publicationStatus = PublicationStatus.valueOf(record.get(Book.BOOK.PUBLICATION_STATUS)),
                    authors = findAuthorsByBookId(bookId)
                )
            }
            .orElse(null)
    }

    /**
     * 著者IDで書籍を検索
     *
     * @param authorId 著者ID
     * @return 指定された著者が執筆した書籍のリスト
     */
    fun findByAuthorId(authorId: Int): List<BookDto> {
        return dsl.select(
            Book.BOOK.ID,
            Book.BOOK.TITLE,
            Book.BOOK.PRICE,
            Book.BOOK.PUBLICATION_STATUS
        )
            .from(Book.BOOK)
            .join(BookAuthor.BOOK_AUTHOR).on(Book.BOOK.ID.eq(BookAuthor.BOOK_AUTHOR.BOOK_ID))
            .where(BookAuthor.BOOK_AUTHOR.AUTHOR_ID.eq(authorId))
            .fetch { record ->
                val bookId = record.get(Book.BOOK.ID)!!
                BookDto(
                    id = bookId,
                    title = record.get(Book.BOOK.TITLE),
                    price = record.get(Book.BOOK.PRICE),
                    publicationStatus = PublicationStatus.valueOf(record.get(Book.BOOK.PUBLICATION_STATUS)),
                    authors = findAuthorsByBookId(bookId)
                )
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
    fun create(title: String, price: Int, authorIds: List<Int>, publicationStatus: PublicationStatus): BookDto {
        val bookId = dsl.insertInto(Book.BOOK)
            .columns(Book.BOOK.TITLE, Book.BOOK.PRICE, Book.BOOK.PUBLICATION_STATUS)
            .values(title, price, publicationStatus.name)
            .returningResult(Book.BOOK.ID)
            .fetchSingle()
            .get(Book.BOOK.ID)!!

        // 著者との関連付け
        authorIds.forEach { authorId ->
            dsl.insertInto(BookAuthor.BOOK_AUTHOR)
                .columns(BookAuthor.BOOK_AUTHOR.BOOK_ID, BookAuthor.BOOK_AUTHOR.AUTHOR_ID)
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
    fun update(id: Int, title: String, price: Int, publicationStatus: PublicationStatus, authorIds: List<Int>): BookDto? {
        // 書籍情報の更新
        val book = dsl.update(Book.BOOK)
            .set(Book.BOOK.TITLE, title)
            .set(Book.BOOK.PRICE, price)
            .set(Book.BOOK.PUBLICATION_STATUS, publicationStatus.name)
            .where(Book.BOOK.ID.eq(id))
            .returningResult(Book.BOOK.ID, Book.BOOK.TITLE, Book.BOOK.PRICE, Book.BOOK.PUBLICATION_STATUS)
            .fetchOne { record ->
                BookDto(
                    id = record.get(Book.BOOK.ID),
                    title = record.get(Book.BOOK.TITLE),
                    price = record.get(Book.BOOK.PRICE),
                    publicationStatus = PublicationStatus.valueOf(record.get(Book.BOOK.PUBLICATION_STATUS))
                )
            } ?: return null

        // 著者関連付けを差分更新し、不要な削除・再挿入を避ける
        val currentAuthorIds = dsl.select(BookAuthor.BOOK_AUTHOR.AUTHOR_ID)
            .from(BookAuthor.BOOK_AUTHOR)
            .where(BookAuthor.BOOK_AUTHOR.BOOK_ID.eq(id))
            .fetch(BookAuthor.BOOK_AUTHOR.AUTHOR_ID)
            .toSet()

        // 差分を計算するために新しい著者IDをセットに変換
        val newAuthorIds = authorIds.toSet()

        // 削除が必要な著者
        (currentAuthorIds - newAuthorIds).forEach { authorId ->
            dsl.deleteFrom(BookAuthor.BOOK_AUTHOR)
                .where(BookAuthor.BOOK_AUTHOR.BOOK_ID.eq(id).and(BookAuthor.BOOK_AUTHOR.AUTHOR_ID.eq(authorId)))
                .execute()
        }

        // 追加が必要な著者
        (newAuthorIds - currentAuthorIds).forEach { authorId ->
            dsl.insertInto(BookAuthor.BOOK_AUTHOR)
                .columns(BookAuthor.BOOK_AUTHOR.BOOK_ID, BookAuthor.BOOK_AUTHOR.AUTHOR_ID)
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
            Author.AUTHOR.ID,
            Author.AUTHOR.NAME,
            Author.AUTHOR.BIRTH_DATE
        )
            .from(Author.AUTHOR)
            .join(BookAuthor.BOOK_AUTHOR).on(Author.AUTHOR.ID.eq(BookAuthor.BOOK_AUTHOR.AUTHOR_ID))
            .where(BookAuthor.BOOK_AUTHOR.BOOK_ID.eq(bookId))
            .fetch { record ->
                AuthorDto(
                    id = record.get(Author.AUTHOR.ID),
                    name = record.get(Author.AUTHOR.NAME),
                    birthDate = record.get(Author.AUTHOR.BIRTH_DATE)
                )
            }
    }
}
