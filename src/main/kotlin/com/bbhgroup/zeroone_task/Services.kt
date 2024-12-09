package com.bbhgroup.zeroone_task

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*


interface UserService {}
interface MessageService {
    fun saveClientMessages(queueClientId: Long, sessionId: Long)
    fun saveOperatorMessage(request: MessageDto)
    fun deleteMessage(id: Long, clientId: Long)
    fun findAllByClientId(clientId: Long): List<MessageResponse>
    fun findAllByOperatorId(operatorId: Long): List<MessageResponse>
}

interface SessionService {}
interface QueueService {
    fun saveQueueMessage(request: MessageQueueDto)
    fun updateQueueMessage(id: Long, clientId: Long, request: UpdateMessageQueueDto)
    fun deleteQueueMessage(id: Long, clientId: Long)
    fun findAllByClientId(clientId: Long): MessageQueueResponse
    fun findFirstQueueClientId(): Long
}

interface RatingService {}

@Service
class UserServiceImpl(private val userRepository: UserRepository) : UserService {

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
        val queue = queueRepository.findFirstByDeletedFalseOrderByPositionAsc() ?: throw EmptyQueueClientIdException()
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
    override fun saveClientMessages(queueClientId: Long, sessionId: Long) {
        val queueList = queueRepository.findAllByClientIdAndDeletedFalseOrderByCreatedAtAsc(queueClientId)
        if (queueList.isEmpty()) throw EmptyListMException()
        val session = sessionRepository.findByIdAndDeletedFalse(sessionId) ?: throw SessionNotFoundMException()
        if (session.client.id != queueClientId) throw InvalidSessionClientIdException()
        queueList.forEach {
            messageRepository.save(
                MessagesEntity(
                    it.client,
                    it.text,
                    it.fileId,
                    it.messageType,
                    session
                )
            )
        }
    }

    override fun saveOperatorMessage(request: MessageDto) {
        TODO("Not yet implemented")
    }

    override fun deleteMessage(id: Long, clientId: Long) {
        TODO("Not yet implemented")
    }

    override fun findAllByClientId(clientId: Long): List<MessageResponse> {
        TODO("Not yet implemented")
    }

    override fun findAllByOperatorId(operatorId: Long): List<MessageResponse> {
        TODO("Not yet implemented")
    }
}

@Service
class SessionServiceImpl(private val sessionRepository: SessionRepository) : SessionService {}

@Service
class RatingServiceImpl(private val ratingRepository: RatingRepository) : RatingService {}
