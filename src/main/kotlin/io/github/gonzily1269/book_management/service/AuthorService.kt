package io.github.gonzily1269.book_management.service

import io.github.gonzily1269.book_management.dto.AuthorCreateRequest
import io.github.gonzily1269.book_management.dto.AuthorDto
import io.github.gonzily1269.book_management.dto.AuthorUpdateRequest
import io.github.gonzily1269.book_management.repository.AuthorRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 著者サービス
 *
 * 著者に関するビジネスロジックを提供するサービスクラス。
 * トランザクション管理を行い、著者の作成・更新を行う。
 *
 * @property authorRepository 著者リポジトリ
 */
@Service
@Transactional
class AuthorService(private val authorRepository: AuthorRepository) {

    /**
     * 新しい著者を作成
     *
     * @param request 著者作成リクエスト
     * @return 作成された著者DTO
     */
    fun createAuthor(request: AuthorCreateRequest): AuthorDto {
        return authorRepository.create(request.name, request.birthDate)
    }

    /**
     * 著者情報を更新
     *
     * @param id 著者ID
     * @param request 著者更新リクエスト
     * @return 更新された著者DTO、見つからない場合はnull
     */
    fun updateAuthor(id: Int, request: AuthorUpdateRequest): AuthorDto? {
        return authorRepository.update(id, request.name, request.birthDate)
    }

}

