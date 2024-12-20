package com.bbhgroup.zeroone_task

import java.util.*

data class BaseMessage(val code: Int, val message: String?)

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

data class MessageDateBetweenDto(
    val beginDate: Date,
    val endDate: Date?,
    val sessionId: Long?,
    val clientId: Long?,
    val operatorId: Long?,
)

data class UserCreateRequest(
    val fullName: String,
    val phoneNumber: String,
    val chatId: Long,
    val language: MutableSet<Languages>
) {
    fun toEntity(role: Role, status: Status, botSteps: BotSteps): UserEntity {
        return UserEntity(fullName, phoneNumber, chatId, role, language, status, botSteps)
    }
}

data class UserResponse(
    val id: Long,
    val fullName: String,
    val phoneNumber: String,
    val chatId: Long,
    val language: Set<Languages?>
) {
    companion object {
        fun toResponse(userEntity: UserEntity): UserResponse {
            userEntity.run {
                return UserResponse(id!!, fullName, phoneNumber, chatId, language)
            }
        }
    }
}

data class OperatorResponse(
        val id: Long,
        val fullName: String,
        val phoneNumber: String,
        val chatId: Long,
        val avgRate: Double?
)

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
)

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


data class TotalSessionsResponse(
    val totalSessions: Long,
    val totalActiveSessions: Long
)

data class OperatorSessionStatisticsResponse(
    val operatorId: Long,
    val operatorName: String,
    val totalHandledSessions: Long,
    val averageRating: Double,
    val activeSessions: Long
)

data class DetailedRatingResponse(
    val totalRatings: Long,
    val averageRating: Double,
    val detailedRatings: Map<Int, Long>
)

data class UserStatisticsResponse(
    val userId: Long,
    val fullName: String,
    val totalSessions: Long,
    val activeSessions: Long,
    val totalRatings: Long,
    val averageRating: Double
)

data class TopRatedOperatorResponse(
    val operatorId: Long,
    val operatorName: String,
    val averageRating: Double,
    val totalRatings: Long
)



