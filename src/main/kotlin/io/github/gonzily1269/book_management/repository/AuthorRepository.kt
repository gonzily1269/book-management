package io.github.gonzily1269.book_management.repository

import io.github.gonzily1269.book_management.dto.AuthorDto
import io.github.gonzily1269.tables.Author
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDate

/**
 * 著者リポジトリ
 *
 * データベースから著者情報を取得・操作するためのリポジトリクラス。
 * jOOQを使用してSQLを実行し、著者情報を管理する。
 *
 * @property dsl jOOQのDSLコンテキスト
 */
@Repository
class AuthorRepository(
    private val dsl: DSLContext
) {

    /**
     * 新しい著者を作成
     *
     * @param name 著者名
     * @param birthDate 生年月日
     * @return 作成された著者DTO
     */
    fun create(name: String, birthDate: LocalDate): AuthorDto {
        return dsl.insertInto(Author.AUTHOR)
            .columns(Author.AUTHOR.NAME, Author.AUTHOR.BIRTH_DATE)
            .values(name, birthDate)
            .returningResult(Author.AUTHOR.ID, Author.AUTHOR.NAME, Author.AUTHOR.BIRTH_DATE)
            .fetchSingle { record ->
                AuthorDto(
                    id = record.get(Author.AUTHOR.ID),
                    name = record.get(Author.AUTHOR.NAME),
                    birthDate = record.get(Author.AUTHOR.BIRTH_DATE)
                )
            }
    }

    /**
     * 著者情報を更新
     *
     * @param id 著者ID
     * @param name 更新する著者名
     * @param birthDate 更新する生年月日
     * @return 更新された著者DTO、見つからない場合はnull
     */
    fun update(id: Int, name: String, birthDate: LocalDate): AuthorDto? {
        return dsl.update(Author.AUTHOR)
            .set(Author.AUTHOR.NAME, name)
            .set(Author.AUTHOR.BIRTH_DATE, birthDate)
            .where(Author.AUTHOR.ID.eq(id))
            .returningResult(Author.AUTHOR.ID, Author.AUTHOR.NAME, Author.AUTHOR.BIRTH_DATE)
            .fetchOne { record ->
                AuthorDto(
                    id = record.get(Author.AUTHOR.ID),
                    name = record.get(Author.AUTHOR.NAME),
                    birthDate = record.get(Author.AUTHOR.BIRTH_DATE)
                )
            }
    }
}
