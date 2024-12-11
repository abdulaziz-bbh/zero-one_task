package com.bbhgroup.zeroone_task

import jakarta.annotation.Nonnull


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
    val messageType: MessageType?,
    val sessionId: Long,
    val replyMessageId: Int?,
    val fileId: String? = null,
) {
    fun toEntity(clientOrOperator: UserEntity, session: Session, replyMessage: Int?): MessagesEntity {
        return MessagesEntity(

            user = clientOrOperator,
            session = session,
            text = text,
            fileId = fileId,
            messageType = messageType!!,
            replyToMessageId = replyMessage
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

data class UserCreateRequest(
        val fullName: String,
        val phoneNumber: String,
        val chatId: Long,
        val language: Set<Languages>
) {
    fun toEntity(role: Role, botSteps: BotSteps): UserEntity {
        return UserEntity(fullName, phoneNumber, chatId, role, language, botSteps)
    }
}

data class UserResponse(

    val id: Long,
    val fullName: String,
    val phoneNumber: String,
    val chatId: Long,
    val language: Set<Languages?>
){
    companion object{
        fun toResponse(userEntity: UserEntity):UserResponse{
            userEntity.run {
                return UserResponse(id!!, fullName, phoneNumber, chatId, language)
            }
        }
    }
}

data class UserUpdateRequest(
        var fullName: String?,
        var phoneNumber: String?,
        var language: Set<Languages>?
)

data class SessionCreateRequest(
        val userId: Long,
        val operatorId: Long,
        val rate: Int,
        val commentForRate: String
) {
    fun toEntity(userId: UserEntity, operatorId: UserEntity): Session {
        return Session(userId, true, operatorId, rate, commentForRate)
    }
}

data class SessionResponse(
        val id: Long,
        val userId: UserResponse,
        val operatorId: UserResponse,
        val active: Boolean
) {
    companion object {
        fun toResponse(session: Session): SessionResponse {
            session.run {
                return SessionResponse(
                        id = this.id!!,
                        userId = UserResponse.toResponse(this.client),
                        operatorId = UserResponse.toResponse(this.operator!!),
                        active = true
                )
            }
        }
    }
}
