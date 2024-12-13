package com.bbhgroup.zeroone_task

import jakarta.persistence.EntityManager
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

interface UserService {
    fun create(request: UserCreateRequest)
    fun getOne(id: Long): UserResponse
    fun deleteOne(id: Long)
    fun getAll(
        role: String?,
        startTime: String?,
        endTime: String?,
        pageable: Pageable
    ): Page<UserResponse>

    fun update(id: Long, request: UserUpdateRequest)
    fun changeRole(id: Long, role: String)
}


interface MessageService {
    fun saveClientOperatorMessage(request: MessageDto)
    fun deleteMessage(id: Long, clientId: Long)
    fun findAllBySessionId(sessionId: Long): MessageSessionResponse
    fun findAllByClientId(clientId: Long): List<MessageSessionResponse>
    fun findAllByOperatorId(operatorId: Long): List<MessageSessionResponse>
}

interface SessionService {
    fun create(request: SessionCreateRequest)
    fun getOne( id: Long):SessionResponse
    fun deleteOne(id: Long)
    fun getAll(startTime: String?,
               endTime: String?,
               pageable: Pageable):Page<SessionResponse>
}

interface StatisticsService {
    fun getTotalSessions(): TotalSessionsResponse
    fun getOperatorSessionStatistics(operatorId: Long): OperatorSessionStatisticsResponse
    fun getDetailedRatings(): DetailedRatingResponse
    fun getUserStatistics(): List<UserStatisticsResponse>
    fun getTopRatedOperators(lastMonth: Boolean = true, limit: Int = 10): List<TopRatedOperatorResponse>
}

@Service
class UserServiceImpl(private val userRepository: UserRepository) : UserService {
    override fun create(request: UserCreateRequest) {
        request.run {
            val user = userRepository.findUserEntityByChatIdAndDeletedFalse(chatId)
            if (user != null) throw UserHasAlreadyExistsException()
            userRepository.save(this.toEntity(Role.USER, BotSteps.START))
        }
    }

    override fun getOne(id: Long): UserResponse {
        return userRepository.findByIdAndDeletedFalse(id)?.let {
            UserResponse.toResponse(it)
        } ?: throw UserNotFoundException()
    }

    override fun deleteOne(id: Long) {
        userRepository.trash(id) ?: throw UserNotFoundException()
    }

    override fun getAll(
        role: String?,
        startTime: String?,
        endTime: String?,
        pageable: Pageable
    ): Page<UserResponse> {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val start = startTime?.let { LocalDate.parse(it, formatter).atStartOfDay() }
        val end = endTime?.let { LocalDate.parse(it, formatter).atTime(23, 59, 59) }
        val validRole = role?.let {
            try {
                Role.valueOf(it.uppercase(Locale.getDefault()))
            } catch (e: IllegalArgumentException) {
                throw UserBadRequestException()
            }
        }
        val users = when {
            validRole != null && start != null && end != null ->
                userRepository.findByRoleAndCreatedAtBetween(validRole, start, end, pageable)

            validRole != null ->
                userRepository.findUserEntityByRoleAndDeletedFalse(validRole, pageable)

            start != null && end != null ->
                userRepository.findUserEntityByCreatedAtBetween(start, end, pageable)

            else ->
                userRepository.findAllNotDeletedForPageable(pageable)
        }
        return users.map { user ->
            UserResponse(
                id = user.id ?: throw IllegalStateException("User ID cannot be null"),
                fullName = user.fullName,
                phoneNumber = user.phoneNumber,
                chatId = user.chatId,
                language = user.language
            )
        }
    }

    override fun update(id: Long, request: UserUpdateRequest) {
        val user = userRepository.findByIdAndDeletedFalse(id) ?: throw UserNotFoundException()
        request.run {
            phoneNumber?.let {
                val usernameAndDeletedFalse = userRepository.findUserEntityByPhoneNumberAndDeletedFalse(id, it)
                if (usernameAndDeletedFalse != null) throw UserHasAlreadyExistsException()
                user.phoneNumber = it
            }
            fullName?.let { request.fullName = it }
            language?.let { request.language = it }
        }
        userRepository.save(user)
    }

    override fun changeRole(id: Long, role: String) {
        val validRole = try {
            Role.valueOf(role.uppercase(Locale.getDefault()))
        } catch (e: IllegalArgumentException) {
            throw UserBadRequestException()
        }
        val userEntity = userRepository.findByIdAndDeletedFalse(id) ?: throw UserNotFoundException()
        userEntity.role = validRole
        userRepository.save(userEntity)
    }
}

@Service
class MessageServiceImpl(
    private val messageRepository: MessageRepository,
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository,
) : MessageService {

    @Transactional
    override fun saveClientOperatorMessage(request: MessageDto) {
        val clientOrOperator =
            userRepository.findByIdAndDeletedFalse(request.clientOrOperatorId) ?: throw UserNotFoundException()
        val session =
            sessionRepository.findByIdAndDeletedFalse(request.sessionId) ?: throw SessionNotFoundMException()
        if (
            session.operator!!.id != clientOrOperator.id ||
            session.client.id != clientOrOperator.id
        ) throw InvalidSessionClientIdException()

        if (request.text != null && request.messageType != MessageType.TEXT) throw InvalidMessageTypeException()
        if (request.fileId != null && request.messageType == MessageType.TEXT) throw InvalidMessageTypeException()

        messageRepository.save(request.toEntity(clientOrOperator, session, request.replyMessageId))
    }

    @Transactional
    override fun deleteMessage(id: Long, clientId: Long) {
        val message = messageRepository.findByIdAndDeletedFalse(id) ?: throw MessageNotFoundException()
        if (message.session.client.id != clientId || message.session.operator!!.id != clientId) throw InvalidSessionClientIdException()
        messageRepository.trash(id)
    }

    override fun findAllBySessionId(sessionId: Long): MessageSessionResponse {
        val session = sessionRepository.findByIdAndDeletedFalse(sessionId) ?: throw SessionNotFoundMException()

        val messageList = messageRepository.findAllBySessionIdAndDeletedFalseOrderByCreatedAtAsc(sessionId)
        if (messageList.isEmpty()) throw EmptyListMException()

        return MessageSessionResponse.toResponse(session, messageList)
    }

    override fun findAllByClientId(clientId: Long): List<MessageSessionResponse> {
        val sessionList = sessionRepository.findAllByClientIdAndDeletedFalseOrderByCreatedAtDesc(clientId)
        if (sessionList.isEmpty()) throw EmptyListMException()

        return sessionList.map { session ->
            val messageList = messageRepository.findAllBySessionIdAndDeletedFalseOrderByCreatedAtAsc(session.id!!)
            MessageSessionResponse.toResponse(session, messageList)
        }.toList()
    }

    override fun findAllByOperatorId(operatorId: Long): List<MessageSessionResponse> {
        val sessionList = sessionRepository.findAllByOperatorIdAndDeletedFalseOrderByCreatedAtDesc(operatorId)
        if (sessionList.isEmpty()) throw EmptyListMException()

        return sessionList.map { session ->
            val messageList = messageRepository.findAllBySessionIdAndDeletedFalseOrderByCreatedAtAsc(session.id!!)
            MessageSessionResponse.toResponse(session, messageList)
        }.toList()
    }

    @Service
    class SessionServiceImpl(
        private val sessionRepository: SessionRepository,
        private val userRepository: UserRepository
    ) : SessionService {
        @Transactional
        override fun create(request: SessionCreateRequest) {
            val user = userRepository.findByIdAndDeletedFalse(request.userId) ?: throw UserNotFoundException()
            val operator = userRepository.findUserEntityByIdAndRoleAndDeletedFalse(request.operatorId, Role.OPERATOR)
                ?: throw UserNotFoundException()
            val session = Session(
                client = user,
                operator = operator,
                isActive = true,
                rate = request.rate,
                commentForRate = request.commentForRate
            )
            sessionRepository.save(session)
        }

        override fun getOne(id: Long): SessionResponse {
            return sessionRepository.findByIdAndDeletedFalse(id)?.let {
                SessionResponse.toResponse(it)
            } ?: throw SessionNotFoundMException()
        }

        override fun deleteOne(id: Long) {
            sessionRepository.trash(id) ?: throw SessionNotFoundMException()
        }

        override fun getAll(
            startTime: String?,
            endTime: String?,
            pageable: Pageable
        ): Page<SessionResponse> {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val start = startTime?.let { LocalDate.parse(it, formatter).atStartOfDay() }
            val end = endTime?.let { LocalDate.parse(it, formatter).atTime(23, 59, 59) }
            val sessions = when {
                start != null && end != null ->
                    sessionRepository.findSessionByCreatedAtBetween(start, end, pageable)

                else ->
                    sessionRepository.findAllNotDeletedForPageable(pageable)
            }
            return sessions.map { session ->
                SessionResponse(
                    id = session.id ?: throw IllegalStateException("Session ID cannot be null"),
                    userId = UserResponse.toResponse(session.client),
                    operatorId = UserResponse.toResponse(session.operator!!),
                    active = true
                )
            }
        }
    }
}



@Service
class StatisticsServiceImpl(
    private val sessionRepository: SessionRepository,
    private val userRepository: UserRepository,
    private val statisticsMapper: StatisticsMapper
) : StatisticsService {


    override fun getTotalSessions(): TotalSessionsResponse {
        val totalSessions = sessionRepository.count()
        val totalActiveSessions = sessionRepository.countByIsActiveTrue()
        return statisticsMapper.toTotalSessionsResponse(totalSessions, totalActiveSessions)
    }

    override fun getOperatorSessionStatistics(operatorId: Long): OperatorSessionStatisticsResponse {
        val operator = userRepository.findByIdAndRoleAndDeletedFalse(operatorId, Role.OPERATOR)
            ?: throw UserNotFoundException()
        val sessions = sessionRepository.findByOperatorId(operatorId)
        return statisticsMapper.toOperatorSessionStatisticsResponse(
            operatorId = operatorId,
            operatorName = operator.fullName,
            sessions = sessions
        )
    }


    override fun getDetailedRatings(): DetailedRatingResponse {
        val allSessions = sessionRepository.findAll()
        return statisticsMapper.toDetailedRatingResponse(allSessions)
    }

    override fun getUserStatistics(): List<UserStatisticsResponse> {
        val users = userRepository.findAll()
        return users.map { user ->
            val sessions = sessionRepository.findByClientId(user.id!!)
            statisticsMapper.toUserStatisticsResponse(user, sessions)
        }
    }

    override fun getTopRatedOperators(lastMonth: Boolean, limit: Int): List<TopRatedOperatorResponse> {
        val now = LocalDateTime.now()
        val startDate = if (lastMonth) now.minusMonths(1) else LocalDateTime.MIN
        val sessions = sessionRepository.findSessionsByCreatedAtBetween(startDate, now)

        val operatorRatings = sessions.filter { it.operator != null }
            .groupBy { it.operator!! }
            .map { (operator, sessions) ->
                val ratings = sessions.mapNotNull { it.rate }
                val totalRatings = ratings.size.toLong()
                val averageRating = if (ratings.isNotEmpty()) ratings.average() else 0.0

                statisticsMapper.toTopRatedOperatorResponse(
                    operator = operator,
                    averageRating = averageRating,
                    totalRatings = totalRatings
                )
            }
        return operatorRatings
            .sortedByDescending { it.averageRating }
            .take(limit)
    }

}
