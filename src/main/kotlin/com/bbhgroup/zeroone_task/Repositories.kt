package com.bbhgroup.zeroone_task

import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.support.JpaEntityInformation
import org.springframework.data.jpa.repository.support.SimpleJpaRepository
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@NoRepositoryBean
interface BaseRepository<T : BaseEntity> : JpaRepository<T, Long>, JpaSpecificationExecutor<T> {
    fun findByIdAndDeletedFalse(id: Long): T?
    fun trash(id: Long): T?
    fun trashList(ids: List<Long>): List<T?>
    fun findAllNotDeleted(): List<T>
    fun findAllNotDeleted(pageable: Pageable): List<T>
    fun findAllNotDeletedForPageable(pageable: Pageable): Page<T>
    fun saveAndRefresh(t: T): T
}

class BaseRepositoryImpl<T : BaseEntity>(
        entityInformation: JpaEntityInformation<T, Long>,
        private val entityManager: EntityManager
) : SimpleJpaRepository<T, Long>(entityInformation, entityManager), BaseRepository<T> {

    val isNotDeletedSpecification = Specification<T> { root, _, cb -> cb.equal(root.get<Boolean>("deleted"), false) }

    override fun findByIdAndDeletedFalse(id: Long) = findByIdOrNull(id)?.run { if (deleted) null else this }

    @Transactional
    override fun trash(id: Long): T? = findByIdOrNull(id)?.run {
        deleted = true
        save(this)
    }

    override fun findAllNotDeleted(): List<T> = findAll(isNotDeletedSpecification)
    override fun findAllNotDeleted(pageable: Pageable): List<T> = findAll(isNotDeletedSpecification, pageable).content
    override fun findAllNotDeletedForPageable(pageable: Pageable): Page<T> = findAll(isNotDeletedSpecification, pageable)

    @Transactional
    override fun trashList(ids: List<Long>): List<T?> = ids.map { trash(it) }


    @Transactional
    override fun saveAndRefresh(t: T): T {
        return save(t).apply { entityManager.refresh(this) }
    }
}

@Repository
interface UserRepository : BaseRepository<UserEntity> {
    @Query("select u from users as u where u.chatId=:chatId and u.deleted=false")
    fun findUserEntityByChatIdAndDeletedFalse(chatId: Long): UserEntity?
    @Query("select u from users as u where u.deleted=false and u.phoneNumber=:phoneNumber and u.id=:id")
    fun findUserEntityByPhoneNumberAndDeletedFalse(id: Long, phoneNumber: String): UserEntity?
    @Query("select u from users u where u.role=:role and u.createdAt between :startTime and :endTime")
    fun findByRoleAndCreatedAtBetween(role:Role ,startTime: LocalDateTime, endTime: LocalDateTime, pageable: Pageable): Page<UserEntity>
    @Query("select u from users as u where u.deleted=false and u.role=:role")
    fun findUserEntityByRoleAndDeletedFalse(role: Role, pageable: Pageable):Page<UserEntity>
    @Query("select u from users u where u.createdAt between :startTime and :endTime")
    fun findUserEntityByCreatedAtBetween(startTime: LocalDateTime, endTime: LocalDateTime, pageable: Pageable):Page<UserEntity>

    fun existsByChatId(chatId: Long): Boolean
}

@Repository
interface SessionRepository : BaseRepository<Session>{

    @Query("""
        select s from Session s where s.client.chatId = :chatId and s.isActive = true
    """)
    fun findByChatIdAndIsActiveTrue(chatId: Long): Session?
}

@Repository
interface QueueRepository : BaseRepository<QueueEntity>

@Repository
interface MessageRepository : BaseRepository<MessagesEntity>

@Repository
interface RatingRepository : BaseRepository<RatingEntity>
