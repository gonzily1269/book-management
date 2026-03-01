package io.github.gonzily1269.book_management.service

import io.github.gonzily1269.book_management.dto.AuthorCreateRequest
import io.github.gonzily1269.book_management.dto.AuthorUpdateRequest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * AuthorServiceのテストクラス
 *
 * 著者サービスの著者作成・更新機能をテストする。
 * リポジトリを経由してDB操作を実行する。
 */
@SpringBootTest
@Transactional
@DisplayName("AuthorServiceのテスト")
class AuthorServiceTest @Autowired constructor(
    private val authorService: AuthorService
) {

    @Test
    @DisplayName("著者作成リクエストから著者を作成できることをテストする")
    fun testCreateAuthor() {
        // Given: 著者作成リクエストを準備
        val request = AuthorCreateRequest(
            name = "太郎",
            birthDate = LocalDate.of(1990, 1, 15)
        )

        // When: 著者を作成
        val result = authorService.createAuthor(request)

        // Then: 著者が正常に作成されたことを検証
        assertNotNull(result.id)
        assertEquals(request.name, result.name)
        assertEquals(request.birthDate, result.birthDate)
    }

    @Test
    @DisplayName("複数の著者を作成できることをテストする")
    fun testCreateMultipleAuthors() {
        // Given: 複数の著者作成リクエスト
        val request1 = AuthorCreateRequest(
            name = "太郎",
            birthDate = LocalDate.of(1990, 1, 15)
        )
        val request2 = AuthorCreateRequest(
            name = "花子",
            birthDate = LocalDate.of(1985, 6, 20)
        )

        // When: 複数の著者を作成
        val result1 = authorService.createAuthor(request1)
        val result2 = authorService.createAuthor(request2)

        // Then: 両方の著者が異なるIDで作成されたことを検証
        assertNotNull(result1.id)
        assertNotNull(result2.id)
        assertEquals(request1.name, result1.name)
        assertEquals(request2.name, result2.name)
    }

    @Test
    @DisplayName("著者情報を更新できることをテストする")
    fun testUpdateAuthor() {
        // Given: 著者を作成してから更新リクエストを準備
        val createRequest = AuthorCreateRequest(
            name = "太郎",
            birthDate = LocalDate.of(1990, 1, 15)
        )
        val author = authorService.createAuthor(createRequest)
        val authorId = author.id!!

        val updateRequest = AuthorUpdateRequest(
            name = "次郎",
            birthDate = LocalDate.of(1995, 3, 10)
        )

        // When: 著者情報を更新
        val result = authorService.updateAuthor(authorId, updateRequest)

        // Then: 著者情報が正常に更新されたことを検証
        assertNotNull(result)
        assertEquals(authorId, result?.id)
        assertEquals(updateRequest.name, result?.name)
        assertEquals(updateRequest.birthDate, result?.birthDate)
    }

    @Test
    @DisplayName("存在しない著者を更新しようとするとnullが返されることをテストする")
    fun testUpdateNonExistentAuthor() {
        // Given: 存在しないID
        val nonExistentId = 99999
        val updateRequest = AuthorUpdateRequest(
            name = "新しい名前",
            birthDate = LocalDate.now()
        )

        // When: 存在しない著者を更新しようとする
        val result = authorService.updateAuthor(nonExistentId, updateRequest)

        // Then: nullが返されることを検証
        assertNull(result)
    }

    @Test
    @DisplayName("複数の著者を作成・更新できることをテストする")
    fun testCreateAndUpdateMultipleAuthors() {
        // Given: 複数の著者を作成
        val author1 = authorService.createAuthor(
            AuthorCreateRequest("太郎", LocalDate.of(1990, 1, 15))
        )
        val author2 = authorService.createAuthor(
            AuthorCreateRequest("花子", LocalDate.of(1985, 6, 20))
        )

        // When: 両方の著者を更新
        val updated1 = authorService.updateAuthor(
            author1.id!!,
            AuthorUpdateRequest("太郎_更新", LocalDate.of(1990, 2, 14))
        )
        val updated2 = authorService.updateAuthor(
            author2.id!!,
            AuthorUpdateRequest("花子_更新", LocalDate.of(1985, 7, 19))
        )

        // Then: 両方の著者が正常に更新されたことを検証
        assertNotNull(updated1)
        assertNotNull(updated2)
        assertEquals("太郎_更新", updated1?.name)
        assertEquals("花子_更新", updated2?.name)
    }

    @Test
    @DisplayName("著者の生年月日を変更して更新できることをテストする")
    fun testUpdateAuthorBirthDate() {
        // Given: 著者を作成
        val author = authorService.createAuthor(
            AuthorCreateRequest("太郎", LocalDate.of(1990, 1, 15))
        )
        val authorId = author.id!!

        // When: 著者の生年月日を更新
        val newBirthDate = LocalDate.of(1991, 5, 20)
        val result = authorService.updateAuthor(
            authorId,
            AuthorUpdateRequest(author.name, newBirthDate)
        )

        // Then: 著者の生年月日が更新されたことを検証
        assertNotNull(result)
        assertEquals(author.name, result?.name)
        assertEquals(newBirthDate, result?.birthDate)
    }

    @Test
    @DisplayName("著者の名前を変更して更新できることをテストする")
    fun testUpdateAuthorName() {
        // Given: 著者を作成
        val author = authorService.createAuthor(
            AuthorCreateRequest("太郎", LocalDate.of(1990, 1, 15))
        )
        val authorId = author.id!!

        // When: 著者の名前を更新
        val newName = "新しい太郎"
        val result = authorService.updateAuthor(
            authorId,
            AuthorUpdateRequest(newName, author.birthDate)
        )

        // Then: 著者の名前が更新されたことを検証
        assertNotNull(result)
        assertEquals(newName, result?.name)
        assertEquals(author.birthDate, result?.birthDate)
    }
}

