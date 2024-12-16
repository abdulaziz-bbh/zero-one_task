package com.bbhgroup.zeroone_task

import jakarta.persistence.*
import org.hibernate.annotations.ColumnDefault
import org.springframework.data.annotation.CreatedDate
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
    @Column(nullable = false, unique = true)
        var phoneNumber: String,
    @Column(nullable = false, unique = true)
        val chatId: Long,
    @Enumerated(EnumType.STRING) var role: Role = Role.USER,
    @ElementCollection(fetch = FetchType.EAGER) @Enumerated(EnumType.STRING) val language: Set<Languages>,
    @Enumerated(EnumType.STRING) var status: Status? = null,
    @Enumerated(EnumType.STRING) var botSteps: BotSteps? = BotSteps.START
) : BaseEntity()

@Entity(name = "messages")
class MessagesEntity(
        @ManyToOne
        val user: UserEntity,
        val text: String? = null,
        val fileId: String? = null,
        val mediaUrl: String? = null,
        val messageId: Int? = null,
        val replyToMessageId: Int? = null,
        val latitude: Double? = null,
        val longitude: Double? = null,
        @Enumerated(EnumType.STRING)
        val messageType: MessageType,
        @ManyToOne
        val session: Session,

) : BaseEntity()

@Entity(name = "sessions")
class Session(
    @ManyToOne
    val client: UserEntity,
    @Enumerated(EnumType.STRING)
    var status: SessionStatus,
    @ManyToOne
    var operator: UserEntity? = null,
    var rate : Int? =null,
) : BaseEntity()
