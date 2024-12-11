package com.bbhgroup.zeroone_task

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
                session.operator!!.id,
                session.operator!!.phoneNumber,
                session.client.id,
                session.client.phoneNumber,
                session.client.language.first(),
                messageList.map { MessageSessionItemResponse.toResponse(session, it) }.toList()
            )
        }
    }
}

data class ReplyMessageResponse(
    val id: Long?,
    val createdAt: String,
    val fileId: String?,
    val text: String?,
    val messageType: MessageType,
) {
    companion object {
        fun toResponse(message: MessagesEntity): ReplyMessageResponse {
            return ReplyMessageResponse(
                message.id,
                message.createdAt.format("dd.MM.yyyy HH:mm:ss"),
                message.fileId,
                message.text,
                message.messageType,
            )
        }
    }
}

data class MessageSessionItemResponse(
    val id: Long?,
    val createdAt: String,
    val fileId: String?,
    val text: String?,
    val messageType: MessageType,
    val isOperatorMessage: Boolean,
    val replyMessage:Int? = null,
) {
    companion object {
        fun toResponse(session: Session, message: MessagesEntity): MessageSessionItemResponse {
            return MessageSessionItemResponse(
                message.id,
                message.createdAt.format("dd.MM.yyyy HH:mm:ss"),
                message.fileId,
                message.text,
                message.messageType,
                session.operator!!.id == message.user.id,
                message.replyToMessageId
            )
        }
    }
}