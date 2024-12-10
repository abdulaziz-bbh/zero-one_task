package com.bbhgroup.zeroone_task

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

interface MessageService {
    fun saveClientQueueMessages(queueClientId: Long, sessionId: Long)
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
interface QueueService {
    fun saveQueueMessage(request: MessageQueueDto)
    fun updateQueueMessage(id: Long, clientId: Long, request: UpdateMessageQueueDto)
    fun deleteQueueMessage(id: Long, clientId: Long)
    fun findAllByClientId(clientId: Long): MessageQueueResponse
    fun findFirstQueueClientId(): Long
}

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
    class QueueServiceImpl(
        private val queueRepository: QueueRepository,
        private val userRepository: UserRepository,
    ) : QueueService {

        @Transactional
        override fun saveQueueMessage(request: MessageQueueDto) {
            val client = userRepository.findByIdAndDeletedFalse(request.clientId) ?: throw UserNotFoundException()
            if (request.messageItemList.isEmpty()) throw EmptyListMException()
            val position = Date().time
            if (request.messageItemList.any {
                    (it.text != null && it.messageType != MessageType.TEXT) ||
                            (it.fileId != null && it.messageType == MessageType.TEXT)
                }) throw InvalidMessageTypeException()
            request.messageItemList.forEach {
                queueRepository.save(
                    QueueEntity(
                        client,
                        it.text,
                        it.fileId,
                        it.messageType,
                        position,
                    )
                )
            }
        }

        @Transactional
        override fun updateQueueMessage(id: Long, clientId: Long, request: UpdateMessageQueueDto) {
            val queue = queueRepository.findByIdAndDeletedFalse(id) ?: throw QueueNotFoundException()
            if (queue.client.id != clientId) throw QueueOwnerException()
            request.messageType?.let {
                if (it != queue.messageType) {
                    queue.messageType = it
                }
            }
            request.fileId?.let {
                if (it != queue.fileId) {
                    if (queue.messageType == MessageType.TEXT) throw InvalidMessageTypeException()
                    queue.fileId = it
                }
            }
            request.text?.let {
                if (queue.messageType != MessageType.TEXT) throw InvalidMessageTypeException()
                queue.text = it
            }
            queueRepository.save(queue)
        }

        @Transactional
        override fun deleteQueueMessage(id: Long, clientId: Long) {
            val queue = queueRepository.findByIdAndDeletedFalse(id) ?: throw QueueNotFoundException()
            if (queue.client.id != clientId) throw QueueOwnerException()
            queueRepository.trash(id)
        }

        override fun findAllByClientId(clientId: Long): MessageQueueResponse {
            val queueList = queueRepository.findAllByClientIdAndDeletedFalseOrderByCreatedAtAsc(clientId)
            if (queueList.isEmpty()) throw EmptyListMException()
            val queueClient = queueList.first().client
            return MessageQueueResponse.toResponse(queueClient, queueList)
        }

        override fun findFirstQueueClientId(): Long {
            val queue =
                queueRepository.findFirstByDeletedFalseOrderByPositionAsc() ?: throw EmptyQueueClientIdException()
            return queue.client.id!!
        }

    }

    @Service
    class MessageServiceImpl(
        private val messageRepository: MessageRepository,
        private val userRepository: UserRepository,
        private val queueRepository: QueueRepository,
        private val sessionRepository: SessionRepository,
    ) : MessageService {

        @Transactional
        override fun saveClientQueueMessages(queueClientId: Long, sessionId: Long) {
            val queueList = queueRepository.findAllByClientIdAndDeletedFalseOrderByCreatedAtAsc(queueClientId)
            if (queueList.isEmpty()) throw EmptyListMException()
            val session = sessionRepository.findByIdAndDeletedFalse(sessionId) ?: throw SessionNotFoundMException()
            if (session.client.id != queueClientId) throw InvalidSessionClientIdException()
            queueList.forEach { queue ->
                messageRepository.save(
                    MessagesEntity(
                        queue.client,
                        queue.text,
                        queue.fileId,
                        queue.messageType,
                        session
                    )
                )
                queueRepository.trash(queue.id!!)
            }
        }

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
    class SessionServiceImpl(
            private val sessionRepository: SessionRepository,
            private val userRepository: UserRepository
    ) : SessionService {
        @Transactional
        override fun create(request: SessionCreateRequest) {
            val user = userRepository.findByIdAndDeletedFalse(request.userId)?:throw UserNotFoundException()
            val operator = userRepository.findUserEntityByIdAndRoleAndDeletedFalse(request.operatorId,Role.OPERATOR)
                    ?: throw UserNotFoundException()
            val session = Session(
                    client = user,
                    operator = operator,
                    active = true,
                    rate = request.rate,
                    commentForRate = request.commentForRate
            )
            sessionRepository.save(session)
        }

        override fun getOne(id: Long): SessionResponse {
            return sessionRepository.findByIdAndDeletedFalse(id)?.let {
                SessionResponse.toResponse(it)
            }?:throw SessionNotFoundMException()
        }

        override fun deleteOne(id: Long) {
            sessionRepository.trash(id)?:throw SessionNotFoundMException()
        }

        override fun getAll(startTime: String?,
                            endTime: String?,
                            pageable: Pageable): Page<SessionResponse> {
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
                        operatorId = UserResponse.toResponse(session.operator),
                        active = true
                )
            }
        }
    }