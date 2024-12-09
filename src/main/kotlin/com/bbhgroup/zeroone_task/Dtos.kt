package com.bbhgroup.zeroone_task


data class BaseMessage(val code: Int, val message: String?)

data class MessageItemDto(
    val text: String?,
    val fileId: Long?,
    val messageType: MessageType,
)

data class MessageQueueDto(
    val clientId: Long,
    val messageItemList: List<MessageItemDto>,
)

data class UpdateMessageQueueDto(
    val text: String?,
    val fileId: Long?,
    val messageType: MessageType?,
)

data class MessageDto(
    val clientOrOperatorId: Long,
    val text: String?,
    val messageType: MessageType,
    val sessionId: Long,
    val replyMessageId: Long?,
    val fileId: Long? = null,
) {
    fun toEntity(clientOrOperator: UserEntity, session: Session, replyMessage: MessagesEntity?): MessagesEntity {
        return MessagesEntity(
            clientOrOperator,
            text,
            fileId,
            messageType,
            session,
            replyMessage,
        )
    }
}

data class UpdateMessageDto(
    val clientId: Long?,
    val text: String?,
    val messageType: MessageType?,
    val sessionId: Long?,
    val fileId: Long? = null,
)


