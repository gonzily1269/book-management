package io.github.gonzily1269.book_management.repository

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
 * AuthorRepositoryのテストクラス
 *
 * 著者リポジトリの著者作成・更新機能をテストする。
 * 実環境と同じPostgreSQLを使用してDB操作をテストする。
 */
@SpringBootTest
@Transactional
@DisplayName("AuthorRepositoryのテスト")
class AuthorRepositoryTest @Autowired constructor(
    private val authorRepository: AuthorRepository
) {

    private val testAuthorName = "太郎"
    private val testBirthDate = LocalDate.of(1990, 1, 15)

    @Test
    @DisplayName("著者を正常に作成できることをテストする")
    fun testCreateAuthor() {
        val name = testAuthorName
        val birthDate = testBirthDate

        val result = authorRepository.create(name, birthDate)

        assertNotNull(result.id)
        assertEquals(name, result.name)
        assertEquals(birthDate, result.birthDate)
    }

    @Test
    @DisplayName("複数の著者を作成できることをテストする")
    fun testCreateMultipleAuthors() {
        val author1Data = Pair("太郎", LocalDate.of(1990, 1, 15))
        val author2Data = Pair("花子", LocalDate.of(1985, 6, 20))

        val result1 = authorRepository.create(author1Data.first, author1Data.second)
        val result2 = authorRepository.create(author2Data.first, author2Data.second)

        assertNotNull(result1.id)
        assertNotNull(result2.id)
        assertEquals(author1Data.first, result1.name)
        assertEquals(author2Data.first, result2.name)
    }

    @Test
    @DisplayName("著者情報を正常に更新できることをテストする")
    fun testUpdateAuthor() {
        val author = authorRepository.create(testAuthorName, testBirthDate)
        val authorId = author.id!!
        val newName = "次郎"
        val newBirthDate = LocalDate.of(1995, 3, 10)

        val result = authorRepository.update(authorId, newName, newBirthDate)

        assertNotNull(result)
        assertEquals(authorId, result.id)
        assertEquals(newName, result.name)
        assertEquals(newBirthDate, result.birthDate)
    }

    @Test
    @DisplayName("存在しない著者を更新しようとするとnullが返されることをテストする")
    fun testUpdateNonExistentAuthor() {
        val nonExistentId = 99999
        val result = authorRepository.update(nonExistentId, "新しい名前", LocalDate.now())
        assertNull(result)
    }

    @Test
    @DisplayName("複数の著者を更新できることをテストする")
    fun testUpdateMultipleAuthors() {
        val author1 = authorRepository.create("太郎", LocalDate.of(1990, 1, 15))
        val author2 = authorRepository.create("花子", LocalDate.of(1985, 6, 20))
        val author1Id = author1.id!!
        val author2Id = author2.id!!

        val updated1 = authorRepository.update(author1Id, "太郎_更新", LocalDate.of(1990, 2, 14))
        val updated2 = authorRepository.update(author2Id, "花子_更新", LocalDate.of(1985, 7, 19))

        assertNotNull(updated1)
        assertNotNull(updated2)
        assertEquals("太郎_更新", updated1.name)
        assertEquals("花子_更新", updated2.name)
    }

    @Test
    @DisplayName("著者の生年月日のみを更新できることをテストする")
    fun testUpdateAuthorBirthDateOnly() {
        val author = authorRepository.create("太郎", LocalDate.of(1990, 1, 15))
        val authorId = author.id!!
        val newBirthDate = LocalDate.of(1991, 5, 20)

        val result = authorRepository.update(authorId, author.name, newBirthDate)

        assertNotNull(result)
        assertEquals(author.name, result.name)
        assertEquals(newBirthDate, result.birthDate)
    }

    @Test
    @DisplayName("著者の名前のみを更新できることをテストする")
    fun testUpdateAuthorNameOnly() {
        val author = authorRepository.create("太郎", LocalDate.of(1990, 1, 15))
        val authorId = author.id!!
        val newName = "新しい太郎"

        val result = authorRepository.update(authorId, newName, author.birthDate)

        assertNotNull(result)
        assertEquals(newName, result.name)
        assertEquals(author.birthDate, result.birthDate)
    }

}

