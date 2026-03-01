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

    private fun createRequest(name: String, birthDate: LocalDate) =
        AuthorCreateRequest(name = name, birthDate = birthDate)

    private fun updateRequest(name: String, birthDate: LocalDate) =
        AuthorUpdateRequest(name = name, birthDate = birthDate)

    @Test
    @DisplayName("著者作成リクエストから著者を作成できることをテストする")
    fun testCreateAuthor() {
        val request = createRequest("太郎", LocalDate.of(1990, 1, 15))
        val result = authorService.createAuthor(request)

        assertNotNull(result.id)
        assertEquals(request.name, result.name)
        assertEquals(request.birthDate, result.birthDate)
    }

    @Test
    @DisplayName("複数の著者を作成できることをテストする")
    fun testCreateMultipleAuthors() {
        val request1 = createRequest("太郎", LocalDate.of(1990, 1, 15))
        val request2 = createRequest("花子", LocalDate.of(1985, 6, 20))

        val result1 = authorService.createAuthor(request1)
        val result2 = authorService.createAuthor(request2)

        assertNotNull(result1.id)
        assertNotNull(result2.id)
        assertEquals(request1.name, result1.name)
        assertEquals(request2.name, result2.name)
    }

    @Test
    @DisplayName("著者情報を更新できることをテストする")
    fun testUpdateAuthor() {
        val createRequest = createRequest("太郎", LocalDate.of(1990, 1, 15))
        val author = authorService.createAuthor(createRequest)
        val authorId = author.id!!

        val updateRequest = updateRequest("次郎", LocalDate.of(1995, 3, 10))
        val result = authorService.updateAuthor(authorId, updateRequest)

        assertNotNull(result)
        assertEquals(authorId, result.id)
        assertEquals(updateRequest.name, result.name)
        assertEquals(updateRequest.birthDate, result.birthDate)
    }

    @Test
    @DisplayName("存在しない著者を更新しようとするとnullが返されることをテストする")
    fun testUpdateNonExistentAuthor() {
        val nonExistentId = 99999
        val updateRequest = updateRequest("新しい名前", LocalDate.now())
        val result = authorService.updateAuthor(nonExistentId, updateRequest)

        assertNull(result)
    }

    @Test
    @DisplayName("複数の著者を作成・更新できることをテストする")
    fun testCreateAndUpdateMultipleAuthors() {
        val author1 = authorService.createAuthor(
            createRequest("太郎", LocalDate.of(1990, 1, 15))
        )
        val author2 = authorService.createAuthor(
            createRequest("花子", LocalDate.of(1985, 6, 20))
        )

        val updated1 = authorService.updateAuthor(
            author1.id!!,
            updateRequest("太郎_更新", LocalDate.of(1990, 2, 14))
        )
        val updated2 = authorService.updateAuthor(
            author2.id!!,
            updateRequest("花子_更新", LocalDate.of(1985, 7, 19))
        )

        assertNotNull(updated1)
        assertNotNull(updated2)
        assertEquals("太郎_更新", updated1.name)
        assertEquals("花子_更新", updated2.name)
    }

    @Test
    @DisplayName("著者の生年月日を変更して更新できることをテストする")
    fun testUpdateAuthorBirthDate() {
        val author = authorService.createAuthor(
            createRequest("太郎", LocalDate.of(1990, 1, 15))
        )
        val authorId = author.id!!

        val newBirthDate = LocalDate.of(1991, 5, 20)
        val result = authorService.updateAuthor(
            authorId,
            updateRequest(author.name, newBirthDate)
        )

        assertNotNull(result)
        assertEquals(author.name, result.name)
        assertEquals(newBirthDate, result.birthDate)
    }

    @Test
    @DisplayName("著者の名前を変更して更新できることをテストする")
    fun testUpdateAuthorName() {
        val author = authorService.createAuthor(
            createRequest("太郎", LocalDate.of(1990, 1, 15))
        )
        val authorId = author.id!!

        val newName = "新しい太郎"
        val result = authorService.updateAuthor(
            authorId,
            updateRequest(newName, author.birthDate)
        )

        assertNotNull(result)
        assertEquals(newName, result.name)
        assertEquals(author.birthDate, result.birthDate)
    }
}

