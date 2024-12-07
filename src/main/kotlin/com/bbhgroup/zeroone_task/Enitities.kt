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
        val username: String,
        val phoneNumber: String,
        val userId: Long,
        @Enumerated(EnumType.STRING) val role: UserRole = UserRole.USER,
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