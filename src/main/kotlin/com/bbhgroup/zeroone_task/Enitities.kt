package com.bbhgroup.zeroone_task

import jakarta.persistence.*
import org.hibernate.annotations.ColumnDefault
import org.springframework.data.annotation.CreatedDate
import java.util.*

@MappedSuperclass
abstract class BaseEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
    @CreatedDate @Temporal(TemporalType.TIMESTAMP) var createdAt: Date?= Date(System.currentTimeMillis()),
    @Column(nullable = false) @ColumnDefault(value = "false") var deleted: Boolean = false,
)