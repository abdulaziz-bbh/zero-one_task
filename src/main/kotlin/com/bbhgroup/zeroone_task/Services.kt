package com.bbhgroup.zeroone_task

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
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

interface SessionService {}


interface RatingService {}


@Service
class UserServiceImpl(private val userRepository: UserRepository) : UserService {
    override fun create(request: UserCreateRequest) {
        request.run {
            val user = userRepository.findUserEntityByChatIdAndDeletedFalse(chatId)
            if (user != null) throw UserHasAlreadyExistsException()
            userRepository.save(this.toEntity(Role.USER))
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
            session.operator.id != clientOrOperator.id ||
            session.client.id != clientOrOperator.id
        ) throw InvalidSessionClientIdException()

        if (request.text != null && request.messageType != MessageType.TEXT) throw InvalidMessageTypeException()
        if (request.fileId != null && request.messageType == MessageType.TEXT) throw InvalidMessageTypeException()
        var replyMessage: MessagesEntity? = null

        request.replyMessageId?.let {
            replyMessage = messageRepository.findByIdAndDeletedFalse(it) ?: throw MessageNotFoundException()
        }
        messageRepository.save(request.toEntity(clientOrOperator, session, replyMessage))
    }

    @Transactional
    override fun deleteMessage(id: Long, clientId: Long) {
        val message = messageRepository.findByIdAndDeletedFalse(id) ?: throw MessageNotFoundException()
        if (message.session.client.id != clientId || message.session.operator.id != clientId) throw InvalidSessionClientIdException()
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
}


@Service
class SessionServiceImpl(private val sessionRepository: SessionRepository) : SessionService {}

@Service
class RatingServiceImpl(private val ratingRepository: RatingRepository) : RatingService {}
