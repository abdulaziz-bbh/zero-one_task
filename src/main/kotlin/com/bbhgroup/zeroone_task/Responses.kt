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

data class MessageResponse(
    val id: Long?,
    val chatId: Long,
    val sessionId: Long?,
    val sessionStatus: Boolean,
    val clientId: Long?,
    val clientPhoneNumber: String,
    val operatorId: Long?,
    val operatorPhoneNumber: String,
    val text: String?,
    val messageType: MessageType,
    val baseLanguage: Languages
) {
    companion object {
        fun toResponse(messagesEntity: MessagesEntity): MessageResponse {
            return MessageResponse(
                messagesEntity.id,
                messagesEntity.client.chatId,
                messagesEntity.session.id,
                messagesEntity.session.active,
                messagesEntity.client.id,
                messagesEntity.client.phoneNumber,
                messagesEntity.session.operator.id,
                messagesEntity.session.operator.phoneNumber,
                messagesEntity.text,
                messagesEntity.messageType,
                //sessionga til qo'shib ketish kerak
                messagesEntity.client.language.first()
            )
        }
    }
}