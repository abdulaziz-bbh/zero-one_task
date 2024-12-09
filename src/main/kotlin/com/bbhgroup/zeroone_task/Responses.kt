package com.bbhgroup.zeroone_task

data class MessageQueueItemResponse(
    val id: Long?,
    val createdAt: String?,
    val fileId: Long?,
    val messageType: MessageType,
    val position: Long,
) {
    companion object {
        fun toResponse(queueEntity: QueueEntity): MessageQueueItemResponse {
            return MessageQueueItemResponse(
                queueEntity.id,
                queueEntity.createdAt.format("dd.MM.yyyy HH:mm:ss"),
                queueEntity.fileId,
                queueEntity.messageType,
                queueEntity.position,
            )
        }
    }
}

data class MessageQueueResponse(
    val clientId: Long?,
    val clientPhoneNumber: String,
    val clientChatId: Long,
    val messageQueueItemList: List<MessageQueueItemResponse>,
) {
    companion object {
        fun toResponse(client: UserEntity, itemList: List<QueueEntity>): MessageQueueResponse {
            return MessageQueueResponse(
                client.id,
                client.phoneNumber,
                client.chatId,
                itemList.map {
                    MessageQueueItemResponse.toResponse(it)
                },
            )
        }
    }
}

data class MessageSessionResponse(
    val sessionId: Long?,
    val sessionOperatorId: Long?,
    val sessionOperatorPhoneNumber: String,
    val sessionClientId: Long?,
    val sessionClientPhoneNumber: String,
    val baseLanguage: Languages,
    val messageItems: List<MessageSessionItemResponse>,
) {
    companion object {
        fun toResponse(session: Session, messageList: List<MessagesEntity>): MessageSessionResponse {
            return MessageSessionResponse(
                session.id,
                session.operator.id,
                session.operator.phoneNumber,
                session.client.id,
                session.client.phoneNumber,
                session.client.language.first(),
                messageList.map { MessageSessionItemResponse.toResponse(session, it) }.toList()
            )
        }
    }
}

data class MessageSessionItemResponse(
    val id: Long?,
    val createdAt: String,
    val fileId: Long?,
    val text: String?,
    val messageType: MessageType,
    val isOperatorMessage: Boolean,
) {
    companion object {
        fun toResponse(session: Session, message: MessagesEntity): MessageSessionItemResponse {
            return MessageSessionItemResponse(
                message.id,
                message.createdAt.format("dd.MM.yyyy HH:mm:ss"),
                message.fileId,
                message.text,
                message.messageType,
                session.operator.id == message.client.id
            )
        }
    }
}