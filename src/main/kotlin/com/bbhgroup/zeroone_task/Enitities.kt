package com.bbhgroup.zeroone_task

import jakarta.persistence.*
import org.hibernate.annotations.ColumnDefault
import org.springframework.data.annotation.CreatedDate
import io.swagger.v3.oas.annotations.media.Schema
import java.util.*

@MappedSuperclass
abstract class BaseEntity(
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
        @CreatedDate @Temporal(TemporalType.TIMESTAMP) var createdAt: Date? = Date(System.currentTimeMillis()),
        @Column(nullable = false) @ColumnDefault(value = "false") var deleted: Boolean = false,
)

@Entity(name = "users")
class UserEntity(
        val fullName: String,
        val username: String,
        val phoneNumber: String,
        val userId: Long,
        @Enumerated(EnumType.STRING) val role: Role = Role.USER,
        @ElementCollection val language: Set<Languages>
) : BaseEntity()



@Entity(name = "messages")
class MessagesEntity(
        val messageId: Long,
        @ManyToOne val user: UserEntity,
        val createdBy : Long
) : BaseEntity()


@Entity(name = "users_inquiries")
class UserInquiries(
        @OneToMany val messages: List<MessagesEntity> = mutableListOf(),
        @ManyToOne val user: UserEntity,
        @Enumerated(EnumType.STRING) val status: InquiriesStatus = InquiriesStatus.PENDING,
        val createdBy : Long
) : BaseEntity()


@Entity(name = "rates")
class RatingEntity(
        val rate: Int,
        val description: String,
        @ManyToOne val user: UserEntity,
        @ManyToOne val operatorId: UserEntity,
        @ManyToOne val userInquiries: UserInquiries
) : BaseEntity()



@Entity(name = "users")
@Schema(description = "Represents a user in the system.")
data class User(

        @Column(nullable = false)
        @Schema(description = "Full name of the user.", example = "John Doe")
        val fullName: String,

        @Column(nullable = false, unique = true)
        @Schema(description = "Chat ID of the user.", example = "123456789")
        val chatId: String,

        @Column(nullable = false, unique = true)
        @Schema(description = "Phone number of the user.", example = "+998901234567")
        val phoneNumber: String,

        @Column(nullable = false)
        @Schema(description = "Password for the user.", example = "password123")
        val password: String,

        @Enumerated(EnumType.STRING)
        @Column(nullable = false)
        @Schema(description = "Role of the user.", example = "USER")
        val role: Role,

        @Column(nullable = false)
        @Schema(description = "Status of the user.", example = "ACTIVE")
        val status: String,

        @Enumerated(EnumType.STRING)
        @Column(nullable = false)
        @Schema(description = "Preferred language of the user.", example = "ENGLISH")
        val language: Languages = Languages.UZ
) : BaseEntity()


@Entity(name = "messages")
@Schema(description = "Represents a message exchanged during a session.")
data class Message(

        @ManyToOne
        @JoinColumn(name = "session_id", nullable = false)
        @Schema(description = "Session associated with the message.")
        val session: Session,

        @ManyToOne
        @JoinColumn(name = "sender_id", nullable = false)
        @Schema(description = "Sender of the message.")
        val sender: User,

        @Column(nullable = false)
        @Schema(description = "Content of the message.", example = "Hello!")
        val text: String,

        @Column(nullable = false)
        @Schema(description = "Type of the message.", example = "TEXT")
        val messageType: String,

        @Schema(description = "File ID associated with the message.", example = "file123")
        val fileId: String? = null
) : BaseEntity()


@Entity(name = "session")
@Schema(description = "Represents a chat session.")
data class Session(

        @ManyToOne
        @JoinColumn(name = "client_id", nullable = false)
        @Schema(description = "Client involved in the session.")
        val client: User,

        @ManyToOne
        @JoinColumn(name = "operator_id")
        @Schema(description = "Operator handling the session, if any.")
        val operator: User? = null,

        @Column(nullable = false)
        @Schema(description = "Indicates whether the session is active.", example = "true")
        val isActive: Boolean = true
) : BaseEntity()


@Entity(name = "queue")
@Schema(description = "Represents the queue of users waiting for assistance.")
data class Queue(

        @ManyToOne
        @JoinColumn(name = "client_id", nullable = false)
        @Schema(description = "Client waiting in the queue.")
        val client: User,

        @Column(nullable = false)
        @Schema(description = "Message associated with the queue entry.", example = "Need help!")
        val message: String,

        @Column(nullable = false)
        @Schema(description = "Position in the queue.", example = "1")
        val position: Int
) : BaseEntity()


@Entity(name = "rates")
@Schema(description = "Represents a user's rating for an operator or application.")
data class Rate(

        @Column(nullable = false)
        @Schema(description = "Application being rated.", example = "Support Bot")
        val application: String,

        @Column(nullable = false)
        @Schema(description = "Comment for the rating.", example = "Great service!")
        val comment: String,

        @ManyToOne
        @JoinColumn(name = "operator_id", nullable = false)
        @Schema(description = "Operator being rated.")
        val operator: User,

        @ManyToOne
        @JoinColumn(name = "user_id", nullable = false)
        @Schema(description = "User who provided the rating.")
        val user: User
) : BaseEntity()









