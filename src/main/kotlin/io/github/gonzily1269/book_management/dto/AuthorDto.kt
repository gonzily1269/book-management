package io.github.gonzily1269.book_management.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PastOrPresent
import java.time.LocalDate

/**
 * 著者のデータ転送オブジェクト
 *
 * @property id 著者ID（新規作成時はnull）
 * @property name 著者名
 * @property birthDate 生年月日
 */
data class AuthorDto(
    val id: Int? = null,
    val name: String,
    val birthDate: LocalDate
)

/**
 * 著者作成リクエスト
 *
 * @property name 著者名（必須、空白不可）
 * @property birthDate 生年月日（必須、現在日以前）
 */
data class AuthorCreateRequest(
    @field:NotBlank(message = "\${validation.author.name.not-blank}")
    val name: String,

    @field:NotNull(message = "\${validation.author.birthDate.not-null}")
    @field:PastOrPresent(message = "\${validation.author.birthDate.past-or-present}")
    val birthDate: LocalDate
)

/**
 * 著者更新リクエスト
 *
 * @property name 著者名（必須、空白不可）
 * @property birthDate 生年月日（必須、現在日以前）
 */
data class AuthorUpdateRequest(
    @field:NotBlank(message = "\${validation.author.name.not-blank}")
    val name: String,

    @field:NotNull(message = "\${validation.author.birthDate.not-null}")
    @field:PastOrPresent(message = "\${validation.author.birthDate.past-or-present}")
    val birthDate: LocalDate
)
