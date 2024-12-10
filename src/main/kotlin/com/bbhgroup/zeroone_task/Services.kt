package com.bbhgroup.zeroone_task

import jakarta.persistence.EntityManager
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*


interface UserService {}
interface MessageService {
    fun saveClientQueueMessages(queueClientId: Long, sessionId: Long)
    fun saveClientOperatorMessage(request: MessageDto)
    fun deleteMessage(id: Long, clientId: Long)
    fun findAllBySessionId(sessionId: Long): MessageSessionResponse
    fun findAllByClientId(clientId: Long): List<MessageSessionResponse>
    fun findAllByOperatorId(operatorId: Long): List<MessageSessionResponse>
}

interface SessionService {}
interface QueueService {
    fun saveQueueMessage(request: MessageQueueDto)
    fun updateQueueMessage(id: Long, clientId: Long, request: UpdateMessageQueueDto)
    fun deleteQueueMessage(id: Long, clientId: Long)
    fun findAllByClientId(clientId: Long): MessageQueueResponse
    fun findFirstQueueClientId(): Long
}
interface RatingService{
    fun getAll(pageable: Pageable): Page<RatingResponse>
    fun getAll(): List<RatingResponse>
    fun getOne(id: Long): RatingResponse
    fun create(request: RatingCreateRequest)
    fun update(id: Long, request: RatingUpdateRequest)
    fun delete(id: Long)
}


@Service
class UserServiceImpl(private val userRepository: UserRepository) : UserService {

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
}

    @Service
    class SessionServiceImpl(private val sessionRepository: SessionRepository) : SessionService {}

    @Service
    class RatingServiceImpl(
        private val ratingRepository: RatingRepository,
        private val userRepository: UserRepository,
        private val ratingMapper: RatingMapper,
        private val sessionRepository: SessionRepository,
        private val entityManager: EntityManager
    ) : RatingService {


        override fun getAll(pageable: Pageable): Page<RatingResponse> {
            return ratingRepository.findAllNotDeletedForPageable(pageable).map {
                ratingMapper.toDto(it)
            }
        }

        override fun getAll(): List<RatingResponse> {
            return ratingRepository.findAllNotDeleted().map {
                ratingMapper.toDto(it)
            }
        }

        override fun getOne(id: Long): RatingResponse {
            ratingRepository.findByIdAndDeletedFalse(id)?.let {
                return ratingMapper.toDto(it)
            } ?: throw RatingNotFoundException()
        }

        override fun create(request: RatingCreateRequest) {
            val existsByClientId = userRepository.existsByClientId(request.clientId)
            if (!existsByClientId) throw UserNotFoundException()
            val existsByOperatorId = userRepository.existsByOperatorId(request.operatorId)
            if (!existsByOperatorId) throw OperatorNotFoundException()
            val existsBySessionId = sessionRepository.existsBySessionId(request.sessionId)
            if (!existsBySessionId) throw SessionNotFoundException()
            val client = entityManager.getReference(
                UserEntity::class.java, request.clientId)
            val operator = entityManager.getReference(
                UserEntity::class.java, request.operatorId)
            val session = entityManager.getReference(
                Session::class.java, request.sessionId)
            ratingRepository.save(ratingMapper.toEntity(request,client,operator,session))
        }

        override fun update(id: Long, request: RatingUpdateRequest) {
            val rating = ratingRepository.findByIdAndDeletedFalse(id) ?: throw RatingNotFoundException()
            request.rate.let { rating.rate = it }
            ratingRepository.save(ratingMapper.updateEntity(rating, request))
        }

        override fun delete(id: Long) {
            ratingRepository.trash(id) ?: throw RatingNotFoundException()
        }




    }
