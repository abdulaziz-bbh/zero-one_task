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
        fun toResponse(
            session: Session,
            messageList: List<MessagesEntity>,
            messageRepository: MessageRepository
        ): MessageSessionResponse {
            return MessageSessionResponse(
                session.id,
                session.operator!!.id,
                session.operator!!.phoneNumber,
                session.client.id,
                session.client.phoneNumber,
                session.client.language.first(),
                messageList.map {
                    var replyM: MessagesEntity? = null
                    it.replyToMessageId?.let { messageId ->
                        replyM = messageRepository.findByNewMessageId(messageId.toLong())
                    }
                    MessageSessionItemResponse.toResponse(session, it, replyM)
                }.toList()
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
    val messageId: Int? = null,
    var newMessageId: Int? = null,
    val mediaUrl: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
) {
    companion object {
        fun toResponse(message: MessagesEntity): ReplyMessageResponse {
            return ReplyMessageResponse(
                message.id,
                message.createdAt.format("dd.MM.yyyy HH:mm:ss"),
                message.fileId,
                message.text,
                message.messageType,
                message.messageId,
                message.newMessageId,
                message.mediaUrl,
                message.latitude,
                message.longitude
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
    val replyMessage: ReplyMessageResponse? = null,
    val messageId: Int? = null,
    var newMessageId: Int? = null,
    val mediaUrl: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
) {
    companion object {
        fun toResponse(
            session: Session,
            message: MessagesEntity,
            replyMessage: MessagesEntity?
        ): MessageSessionItemResponse {
            return MessageSessionItemResponse(
                message.id,
                message.createdAt.format("dd.MM.yyyy HH:mm:ss"),
                message.fileId,
                message.text,
                message.messageType,
                session.operator!!.id == message.user.id,
                replyMessage?.let {
                    ReplyMessageResponse.toResponse(it)
                },
                message.messageId,
                message.newMessageId,
                message.mediaUrl,
                message.latitude,
                message.longitude
            )
        }
    }
}