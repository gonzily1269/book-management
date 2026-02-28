package io.github.gonzily1269.book_management.dto

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Pattern

/**
 * 書籍のデータ転送オブジェクト
 *
 * @property id 書籍ID（新規作成時はnull）
 * @property title 書籍タイトル
 * @property price 書籍価格
 * @property publicationStatus 出版状態（PUBLISHED/UNPUBLISHED）
 * @property authors 著者リスト
 */
data class BookDto(
    val id: Int? = null,
    val title: String,
    val price: Int,
    val publicationStatus: String,
    val authors: List<AuthorDto> = emptyList()
)

/**
 * 書籍作成リクエスト
 *
 * @property title 書籍タイトル（必須、空白不可）
 * @property price 書籍価格（必須、0以上）
 * @property publicationStatus 出版状態（必須、PUBLISHED/UNPUBLISHEDのいずれか）
 * @property authorIds 著者IDリスト（必須、最低1人以上）
 */
data class BookCreateRequest(
    @field:NotBlank(message = "\${validation.book.title.not-blank}")
    val title: String,

    @field:Min(value = 0, message = "\${validation.book.price.min}")
    val price: Int,

    @field:Pattern(
        regexp = "PUBLISHED|UNPUBLISHED",
        message = "\${validation.book.publicationStatus.pattern}"
    )
    val publicationStatus: String,

    @field:NotEmpty(message = "\${validation.book.authorIds.not-empty}")
    val authorIds: List<Int>
)

/**
 * 書籍更新リクエスト
 *
 * @property title 書籍タイトル（必須、空白不可）
 * @property price 書籍価格（必須、0以上）
 * @property publicationStatus 出版状態（必須、PUBLISHED/UNPUBLISHEDのいずれか）
 * @property authorIds 著者IDリスト（必須、最低1人以上）
 */
data class BookUpdateRequest(
    @field:NotBlank(message = "\${validation.book.title.not-blank}")
    val title: String,

    @field:Min(value = 0, message = "\${validation.book.price.min}")
    val price: Int,

    @field:Pattern(
        regexp = "PUBLISHED|UNPUBLISHED",
        message = "\${validation.book.publicationStatus.pattern}"
    )
    val publicationStatus: String,

    @field:NotEmpty(message = "\${validation.book.authorIds.not-empty}")
    val authorIds: List<Int>
)
