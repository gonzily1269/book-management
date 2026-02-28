package io.github.gonzily1269.book_management.repository

import io.github.gonzily1269.book_management.dto.AuthorDto
import org.jooq.DSLContext
import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.table
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DataAccessResourceFailureException
import org.springframework.stereotype.Repository
import java.time.LocalDate

/**
 * 著者リポジトリ
 *
 * データベースから著者情報を取得・操作するためのリポジトリクラス。
 * jOOQを使用してSQLを実行し、著者情報を管理する。
 *
 * @property dsl jOOQのDSLコンテキスト
 * @property createFailedMessage 著者作成失敗時のエラーメッセージ
 */
@Repository
class AuthorRepository(
    private val dsl: DSLContext,

    @Value("\${error.repository.author.create-failed}")
    private val createFailedMessage: String
) {

    /**
     * 新しい著者を作成
     *
     * @param name 著者名
     * @param birthDate 生年月日
     * @return 作成された著者DTO
     * @throws DataAccessResourceFailureException 著者の作成に失敗した場合（DBの不良など）
     */
    fun create(name: String, birthDate: LocalDate): AuthorDto {
        return dsl.insertInto(table("author"))
            .columns(field("name"), field("birth_date"))
            .values(name, birthDate)
            .returningResult(field("id"), field("name"), field("birth_date"))
            .fetchOne { record ->
                AuthorDto(
                    id = record.get("id", Int::class.java),
                    name = record.get("name", String::class.java),
                    birthDate = record.get("birth_date", LocalDate::class.java)
                )
            }
            ?: throw DataAccessResourceFailureException(createFailedMessage)
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
        return dsl.update(table("author"))
            .set(field("name"), name)
            .set(field("birth_date"), birthDate)
            .where(field("id").eq(id))
            .returningResult(field("id"), field("name"), field("birth_date"))
            .fetchOne { record ->
                AuthorDto(
                    id = record.get("id", Int::class.java),
                    name = record.get("name", String::class.java),
                    birthDate = record.get("birth_date", LocalDate::class.java)
                )
            }
    }
}
