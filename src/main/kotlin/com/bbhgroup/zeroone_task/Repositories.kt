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
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.Date

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
    override fun findAllNotDeletedForPageable(pageable: Pageable): Page<T> =
            findAll(isNotDeletedSpecification, pageable)

    @Transactional
    override fun trashList(ids: List<Long>): List<T?> = ids.map { trash(it) }


    @Transactional
    override fun saveAndRefresh(t: T): T {
        return save(t).apply { entityManager.refresh(this) }
    }
}

@Repository
interface UserRepository : BaseRepository<UserEntity> {
    fun countByRole(role: Role): Long

    @Query(
            """
    select count(u) > 0 
    from users u 
    where u.id = :id
    and u.role = 'USER'
"""
    )
    fun existsByClientId(@Param("id") id: Long?): Boolean
    fun findByPhoneNumberAndDeletedFalse(phoneNumber: String): UserEntity?
    @Query("select u from users as u where u.deleted=false and u.role='OPERATOR' and u.status=:status")
    fun findAllByRoleAndDeletedFalseAndStatus(status: Status,pageable: Pageable):Page<UserEntity>


    @Query(
            """
    select count(u) > 0 
    from users u 
    where u.id = :id
    and u.role = 'OPERATOR'
"""
    )
    fun existsByOperatorId(@Param("id") id: Long?): Boolean

    @Query("select u from users u where u.id = :id and u.role = :role and u.deleted = false")
    fun findByIdAndRoleAndDeletedFalse(
        @Param("id") id: Long,
        @Param("role") role: Role
    ): UserEntity?


    @Query("select u from users as u where u.chatId=:chatId and u.deleted=false")
    fun findUserEntityByChatIdAndDeletedFalse(chatId: Long): UserEntity?

    @Query("select u from users as u where u.deleted=false and u.phoneNumber=:phoneNumber and u.id=:id")
    fun findUserEntityByPhoneNumberAndDeletedFalse(id: Long, phoneNumber: String): UserEntity?

    @Query("select u from users u where u.deleted=false and u.role=:role and u.createdAt between :startTime and :endTime")
    fun findByRoleAndCreatedAtBetween(
            role: Role,
            startTime: LocalDateTime,
            endTime: LocalDateTime,
            pageable: Pageable
    ): Page<UserEntity>

    @Query("select u from users as u where u.deleted=false and u.role=:role")
    fun findUserEntityByRoleAndDeletedFalse(role: Role, pageable: Pageable): Page<UserEntity>

    @Query("""  select 
                u.id as id,
                u.fullName as fullName,
                u.phoneNumber as phoneNumber,
                u.chatId as chatId,
                coalesce(avg(session.rate), 0) as avgRate
            from users u
                     left join sessions session on session.operator.id = u.id and session.deleted = false
            where u.role = 'OPERATOR' and u.deleted = false
            group by u.id
            """
    )
    fun findAllByRoleAndDeletedFalse(pageable: Pageable): Page<Map<String, Any>>

    @Query("select u from users as u where u.deleted=false and u.role='OPERATOR' and u.id=:id")
    fun findByIdAndRoleAndDeletedFalse(id: Long): UserEntity?

    @Query("select u from users u where u.deleted=false and u.createdAt between :startTime and :endTime")
    fun findUserEntityByCreatedAtBetween(
            startTime: LocalDateTime,
            endTime: LocalDateTime,
            pageable: Pageable
    ): Page<UserEntity>

    @Query("select u from users as u where u.deleted=false and u.id=:id and u.role=:role")
    fun findUserEntityByIdAndRoleAndDeletedFalse(id: Long, role: Role): UserEntity?


    fun existsByChatId(chatId: Long): Boolean
}

@Repository
interface SessionRepository : BaseRepository<Session> {
    fun findAllByCreatedAtBetweenAndDeletedFalseOrderByCreatedAtDesc(
            beginDate: Date,
            endDate: Date
    ): List<Session>

    fun findAllByIdAndCreatedAtBetweenAndDeletedFalseOrderByCreatedAtDesc(
            sessionId: Long,
            beginDate: Date,
            endDate: Date
    ): List<Session>

    fun findAllByClientIdAndCreatedAtBetweenAndDeletedFalseOrderByCreatedAtDesc(
            clientId: Long,
            beginDate: Date,
            endDate: Date
    ): List<Session>

    fun findAllByOperatorIdAndCreatedAtBetweenAndDeletedFalseOrderByCreatedAtDesc(
            operator: Long,
            beginDate: Date,
            endDate: Date
    ): List<Session>


    @Query("""
        select s from  sessions s where s.client.chatId = :chatId and s.status = 'PENDING' or s.status = 'PROCESSING' order by s.createdAt desc limit 1
    """)
    fun findSessionByChatIdAndStatus(chatId: Long): Session?



    @Query("select s from sessions as s where s.deleted=false and s.operator=:operator")
    fun findAllSessionByOperatorAndDeletedFalse(operator: UserEntity): List<Session>?

    @Query(value = "SELECT * FROM sessions s WHERE s.status = 'PENDING' ORDER BY s.created_at ASC LIMIT 1", nativeQuery = true)
    fun findFirstPendingSession(): Session?

    @Query(value = "select s.* from sessions s join users c on s.client_id = c.id where c.chat_id = :chatId and s.status = 'COMPLETED' order by s.created_at desc limit 1", nativeQuery = true)
    fun findLastCompletedSession(chatId: Long): Session?

    fun findAllByClientIdAndDeletedFalseOrderByCreatedAtDesc(clientId: Long): List<Session>
    fun findAllByOperatorIdAndDeletedFalseOrderByCreatedAtDesc(operatorId: Long): List<Session>


    @Query("select s from sessions s where s.createdAt between :startDate and :endDate and s.deleted = false")
    fun findAllSessionsByCreatedAtBetween(
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): List<Session>

    @Query("select s from sessions s where s.client.id = :clientId")
    fun findByClientId(@Param("clientId") clientId: Long): List<Session>

    @Query("""
        select count(s) from sessions s where s.status = 'COMPLETED'
    """)
    fun countByIsActiveTrue(): Long
  
    fun findByOperatorId(operatorId: Long): List<Session>

    @Query("select s from sessions s where s.createdAt between :startDate and :endDate and s.deleted = false")
    fun findSessionByCreatedAtBetween(startTime: LocalDateTime, endTime: LocalDateTime, pageable: Pageable): Page<Session>

    @Query("""
        select s from sessions s where s.operator.chatId =:chatId and s.status = 'PROCESSING'
    """)
    fun findProcessingSessionsByOperatorId(@Param("chatId") chatId: Long): Session?

    @Query("select s from sessions as s where s.deleted=false and s.status='PROCESSING' and s.operator=:operator")
    fun findAllByStatusAndOperatorAndDeletedFalse(operator: UserEntity): List<UserEntity>?

    @Query("""
        select s from sessions s where s.client.chatId =:chatId and s.status = 'PROCESSING'
    """)
    fun findProcessingSessionsByClientId(@Param("chatId") chatId: Long): Session?

    @Query("select s from sessions as s where s.deleted=false and s.status = 'COMPLETED' order by s.rate asc")
    fun findAllByRateAndDeletedFalseAndActiveTrue(pageable: Pageable): Page<Session>
}

@Repository
interface MessageRepository : BaseRepository<MessagesEntity> {
    fun findAllBySessionIdAndDeletedFalseOrderByCreatedAtAsc(sessionId: Long): List<MessagesEntity>
    fun findAllBySessionIdOrderByCreatedAtAsc(sessionId: Long): List<MessagesEntity>

    @Query("""
        select m from messages m where m.user.chatId = :chatId
    """)
    fun findByUserChatId(chatId: Long): MessagesEntity

    fun findByNewMessageId(newMessageId: Long): MessagesEntity?
    fun findByMessageId(messageId: Long): MessagesEntity?
}

