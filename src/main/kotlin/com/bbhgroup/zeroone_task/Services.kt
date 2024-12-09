package com.bbhgroup.zeroone_task

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*


interface UserService{
    fun create(request: UserCreateRequest)
    fun getOne(id: Long):UserResponse
    fun deleteOne(id: Long)
    fun getAll(role: String?,
               startTime: String?,
               endTime: String?,
               pageable: Pageable): Page<UserResponse>
    fun update(id: Long, request: UserUpdateRequest)
    fun changeRole(id: Long, role: String)
}
interface MessageService{}
interface SessionService{}
interface QueueService{}
interface RatingService{}

@Service
class UserServiceImpl(private val userRepository: UserRepository):UserService{
    override fun create(request: UserCreateRequest) {
        request.run {
            val user = userRepository.findUserEntityByChatIdAndDeletedFalse(chatId)
            if (user != null) throw UserHasAlreadyExistsException()
            userRepository.save(this.toEntity(Role.USER))
        }
    }
    override fun getOne(id: Long): UserResponse {
       return userRepository.findByIdAndDeletedFalse(id)?.let {
            UserResponse.toResponse(it)
        }?:throw UserNotFoundException()
    }
    override fun deleteOne(id: Long) {
        userRepository.trash(id)?:throw UserNotFoundException()
    }
    override fun getAll(
            role: String?,
            startTime: String?,
            endTime: String?,
            pageable: Pageable
    ): Page<UserResponse> {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val start = startTime?.let { LocalDate.parse(it, formatter).atStartOfDay() }
        val end = endTime?.let { LocalDate.parse(it, formatter).atTime(23, 59, 59) }
        val validRole = role?.let {
            try {
                Role.valueOf(it.uppercase(Locale.getDefault()))
            } catch (e: IllegalArgumentException) {
                throw UserBadRequestException()
            }
        }
        val users = when {
            validRole != null && start != null && end != null ->
                userRepository.findByRoleAndCreatedAtBetween(validRole, start, end, pageable)
            validRole != null ->
                userRepository.findUserEntityByRoleAndDeletedFalse(validRole, pageable)
            start != null && end != null ->
                userRepository.findUserEntityByCreatedAtBetween(start, end, pageable)
            else ->
                userRepository.findAllNotDeletedForPageable(pageable)
        }
        return users.map { user ->
            UserResponse(
                    id = user.id ?: throw IllegalStateException("User ID cannot be null"),
                    fullName = user.fullName,
                    phoneNumber = user.phoneNumber,
                    chatId = user.chatId,
                    language = user.language
            )
        }
    }
    override fun update(id: Long, request: UserUpdateRequest) {
        val user = userRepository.findByIdAndDeletedFalse(id) ?: throw UserNotFoundException()
        request.run {
            phoneNumber?.let {
                val usernameAndDeletedFalse = userRepository.findUserEntityByPhoneNumberAndDeletedFalse(id, it)
                if (usernameAndDeletedFalse != null) throw UserHasAlreadyExistsException()
                user.phoneNumber = it
            }
            fullName?.let { request.fullName = it }
            language?.let { request.language = it }
        }
        userRepository.save(user)
    }
    override fun changeRole(id: Long, role: String) {
        val validRole = try {
            Role.valueOf(role.uppercase(Locale.getDefault()))
        } catch (e: IllegalArgumentException) {
            throw UserBadRequestException()
        }
        val userEntity = userRepository.findByIdAndDeletedFalse(id)?:throw UserNotFoundException()
        userEntity.role=validRole
        userRepository.save(userEntity)
    }
}


@Service
class QueueServiceImpl(private val queueRepository: QueueRepository):QueueService{}

@Service
class MessageServiceImpl(private val messageRepository: MessageRepository):MessageService{
}

@Service
class SessionServiceImpl(private val sessionRepository: SessionRepository):SessionService{}

@Service
class RatingServiceImpl(private val ratingRepository: RatingRepository):RatingService{}


