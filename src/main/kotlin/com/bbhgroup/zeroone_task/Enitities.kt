package com.bbhgroup.zeroone_task

import jakarta.persistence.*
import org.hibernate.annotations.ColumnDefault
import org.springframework.data.annotation.CreatedDate
import org.telegram.telegrambots.meta.api.objects.MessageEntity
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
        val phoneNumber: String,
        @Column(nullable = false, unique = true)
        val chatId: Long,
        @Enumerated(EnumType.STRING) val role: UserRole = UserRole.USER,
        @ElementCollection val language: Set<Languages>
) : BaseEntity()
@Entity(name = "messages")
class MessagesEntity(
        @ManyToOne
        val client: UserEntity,
        val text:String,
        val fileId:Long,
        val messageType: MessageType,
        @ManyToOne
        val session: Session

) : BaseEntity()

@Entity
class QueueEntity(
        @ManyToOne
        val client: UserEntity,
        @OneToMany
        val messages: Queue<MessagesEntity>,
        val position: Int
): BaseEntity()

@Entity(name = "rates")
class RatingEntity(
        val rate: Int,
        @ManyToOne val client: UserEntity,
        @ManyToOne val operator: UserEntity,
        @OneToOne val session: Session
) : BaseEntity()

@Entity
class Session(
        @ManyToOne
        val client: UserEntity,
        @ManyToOne
        val operator: UserEntity,
        val active: Boolean
): BaseEntity()