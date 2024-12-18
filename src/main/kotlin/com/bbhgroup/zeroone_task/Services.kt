package com.bbhgroup.zeroone_task

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.telegram.telegrambots.meta.api.methods.GetFile
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Message
import java.net.URL
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

interface UserService {
    fun create(request: UserCreateRequest)
    fun getOne(id: Long): UserResponse
    fun deleteOne(id: Long)
    fun getAll(
            role: String?,
            startTime: String?,
            endTime: String?,
            pageable: Pageable
    ): Page<UserResponse>

    fun update(id: Long, request: UserUpdateRequest)
    fun changeRole(id: Long, role: String)
    fun saveUser(chatId: Long, fullName: String?, language: String?, phone: String?)
    fun existsByChatId(chatId: Long): Boolean
    fun getLanguages(chatId: Long): String
    fun addLanguageToOperator(id: Long, lang: String)
    fun findAllOperatorWithRate(pageable: Pageable): Page<Map<String, Any>>
    fun findOperatorById(id: Long): OperatorResponse
    fun findAllOperatorByStatus(status: String,pageable: Pageable): Page<UserResponse>
}


interface MessageService {
    fun saveClientOperatorMessage(request: MessageDto)
    fun deleteMessage(id: Long, clientId: Long)
    fun findAllBySessionId(sessionId: Long): MessageSessionResponse
    fun findAllByClientId(clientId: Long): List<MessageSessionResponse>
    fun findAllByOperatorId(operatorId: Long): List<MessageSessionResponse>
    fun getFileUrl(fileId: String): String
    fun getFileSize(fileId: String): Long
    fun handleMessage(message: Message, chatId: Long):MessagesEntity
    fun findAllBetweenDates(request: MessageDateBetweenDto):List<MessageSessionResponse>

}

interface SessionService {
    fun create(request: SessionCreateRequest)
    fun getOne(id: Long): SessionResponse
    fun deleteOne(id: Long)
    fun getAll(startTime: String?,
               endTime: String?,
               pageable: Pageable): Page<SessionResponse>

    fun findAllByRate(pageable: Pageable): Page<SessionResponse>
    fun getFirPending(): Session?
}

interface StatisticsService {
    fun getTotalSessions(): TotalSessionsResponse
    fun getOperatorSessionStatistics(operatorId: Long): OperatorSessionStatisticsResponse
    fun getDetailedRatings(): DetailedRatingResponse
    fun getUserStatistics(): List<UserStatisticsResponse>
    fun getTopRatedOperators(lastMonth: Boolean = true, limit: Int = 10): List<TopRatedOperatorResponse>

    // Qo'shilgan methodlar
    fun getTopRatedOperator(): TopRatedOperatorResponse
    fun getLowestRatedOperator(): TopRatedOperatorResponse
    fun getOperatorAverageRatings(): Map<String, Double>
}

@Service
class UserServiceImpl(
        private val userRepository: UserRepository,
        private val sessionRepository: SessionRepository) : UserService {
    override fun create(request: UserCreateRequest) {
        request.run {
            val user = userRepository.findUserEntityByChatIdAndDeletedFalse(chatId)
            if (user != null) throw UserHasAlreadyExistsException()
            userRepository.save(this.toEntity(Role.USER, Status.NOT_WORKING, BotSteps.START))
        }
    }

    override fun getOne(id: Long): UserResponse {
        return userRepository.findByIdAndDeletedFalse(id)?.let {
            UserResponse.toResponse(it)
        } ?: throw UserNotFoundException()
    }

    override fun deleteOne(id: Long) {
        val user = userRepository.findByIdAndDeletedFalse(id) ?: throw UserNotFoundException()
        if (user.role == Role.OPERATOR) {
            sessionRepository.findAllByStatusAndOperatorAndDeletedFalse(user) ?: throw UserBadRequestException()
            user.deleted = true
            userRepository.save(user)
        }
        userRepository.trash(id) ?: throw UserNotFoundException()
    }

    companion object {
        private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }

    override fun getAll(
            role: String?,
            startTime: String?,
            endTime: String?,
            pageable: Pageable
    ): Page<UserResponse> {
        val start = startTime?.let { LocalDate.parse(it, formatter).atStartOfDay() }
        val end = endTime?.let { LocalDate.parse(it, formatter).atTime(23, 59, 59) }
        val validRole = role?.let {
            runCatching { Role.valueOf(it.uppercase(Locale.getDefault())) }
                    .getOrElse { throw UserBadRequestException() }
        }

        val users = when {
            validRole != null && start != null && end != null ->
                findByRoleAndDateRange(validRole, start, end, pageable)

            validRole != null ->
                findByRoleOnly(validRole, pageable)

            start != null && end != null ->
                findByDateRangeOnly(start, end, pageable)

            else -> findAllNotDeleted(pageable)
        }

        return users.map { user ->
            UserResponse(
                    id = user.id ?: error("User ID cannot be null"),
                    fullName = user.fullName,
                    phoneNumber = user.phoneNumber,
                    chatId = user.chatId,
                    language = user.language
            )
        }
    }

    private fun findByRoleAndDateRange(role: Role, start: LocalDateTime, end: LocalDateTime, pageable: Pageable) =
            userRepository.findByRoleAndCreatedAtBetween(role, start, end, pageable)

    private fun findByRoleOnly(role: Role, pageable: Pageable) =
            userRepository.findUserEntityByRoleAndDeletedFalse(role, pageable)

    private fun findByDateRangeOnly(start: LocalDateTime, end: LocalDateTime, pageable: Pageable) =
            userRepository.findUserEntityByCreatedAtBetween(start, end, pageable)

    private fun findAllNotDeleted(pageable: Pageable) =
            userRepository.findAllNotDeletedForPageable(pageable)


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
        val userEntity = userRepository.findByIdAndDeletedFalse(id) ?: throw UserNotFoundException()
        if (userEntity.role == Role.OPERATOR && validRole != Role.OPERATOR) {
            sessionRepository.findAllByStatusAndOperatorAndDeletedFalse(userEntity) ?: throw UserBadRequestException()
        }
        userEntity.role = validRole
        userRepository.save(userEntity)
    }

    override fun saveUser(chatId: Long, fullName: String?, language: String?, phone: String?) {
        val user = UserEntity(
                chatId = chatId,
                fullName = fullName!!,
                language = mutableSetOf(Languages.valueOf(language!!)),
                phoneNumber = phone!!
        )
        userRepository.save(user)
    }

    override fun existsByChatId(chatId: Long): Boolean {
        return userRepository.existsByChatId(chatId)
    }

    @Transactional
    override fun getLanguages(chatId: Long): String {
        val user = userRepository.findUserEntityByChatIdAndDeletedFalse(chatId) ?: throw UserNotFoundException()
        return user.language.first().key
    }

    override fun addLanguageToOperator(id: Long, lang: String) {
        val user = userRepository.findByIdAndDeletedFalse(id) ?: throw UserNotFoundException()
        val language = try {
            Languages.valueOf(lang.uppercase())
        } catch (e: IllegalArgumentException) {
            throw UserBadRequestException()
        }
        if (user.role == Role.OPERATOR) {
            if (user.language.contains(language)) {
                throw DataHasAlreadyExistsException()
            }
            val updatedLanguages = user.language.toMutableSet().apply {
                add(language)
            }
            user.language = updatedLanguages
            userRepository.save(user)
        } else {
            throw UserBadRequestException()
        }
    }

    override fun findAllOperatorWithRate(pageable: Pageable): Page<Map<String, Any>> {
        return userRepository.findAllByRoleAndDeletedFalse(pageable)
    }

    override fun findOperatorById(id: Long): OperatorResponse {
        val operator = userRepository.findByIdAndRoleAndDeletedFalse(id) ?: throw UserNotFoundException()
        if (operator.role != Role.OPERATOR) throw UserBadRequestException()
        val sessions = sessionRepository.findAllSessionByOperatorAndDeletedFalse(operator)
                ?: throw SessionNotFoundException()
        val averageRate = if (sessions.isNotEmpty()) {
            sessions.sumOf { it.rate ?: 0 } / sessions.size
        } else {
            0.0
        }
        return OperatorResponse(
                id = operator.id!!,
                fullName = operator.fullName,
                phoneNumber = operator.phoneNumber,
                chatId = operator.chatId,
                avgRate = averageRate.toDouble()
        )
    }

    override fun findAllOperatorByStatus(status: String, pageable: Pageable): Page<UserResponse> {
        val operatorStatus = try {
            Status.valueOf(status.uppercase())
        } catch (e: IllegalArgumentException) {
            throw UserBadRequestException()
        }
        return userRepository.findAllByRoleAndDeletedFalseAndStatus(operatorStatus,pageable).map {
            UserResponse.toResponse(it)
        }
    }


}

@Service
class MessageServiceImpl(
        private val messageRepository: MessageRepository,
        private val userRepository: UserRepository,
        private val sessionRepository: SessionRepository,
        @Lazy private val botService: BotService
) : MessageService {

    @Value("\${bot.file-api}")
    lateinit var fileApi: String

    @Transactional
    override fun saveClientOperatorMessage(request: MessageDto) {
        val clientOrOperator =
                userRepository.findByIdAndDeletedFalse(request.clientOrOperatorId) ?: throw UserNotFoundException()
        val session =
                sessionRepository.findByIdAndDeletedFalse(request.sessionId) ?: throw SessionNotFoundMException()
        if (
                session.operator!!.id != clientOrOperator.id ||
                session.client.id != clientOrOperator.id
        ) throw InvalidSessionClientIdException()

        if (request.text != null && request.messageType != MessageType.TEXT) throw InvalidMessageTypeException()
        if (request.fileId != null && request.messageType == MessageType.TEXT) throw InvalidMessageTypeException()

        messageRepository.save(request.toEntity(clientOrOperator, session, request.replyMessageId))
    }

    @Transactional
    override fun deleteMessage(id: Long, clientId: Long) {
        val message = messageRepository.findByIdAndDeletedFalse(id) ?: throw MessageNotFoundException()
        if (message.session.client.id != clientId || message.session.operator!!.id != clientId) throw InvalidSessionClientIdException()
        messageRepository.trash(id)
    }

    override fun findAllBetweenDates(request: MessageDateBetweenDto): List<MessageSessionResponse> {
        val endDate = if (request.endDate != null) request.endDate.endOfDateTime() else Date().endOfDateTime()
        val sessionList = if (request.sessionId != null) {
            sessionRepository.findAllByIdAndCreatedAtBetweenAndDeletedFalseOrderByCreatedAtDesc(
                    request.sessionId,
                    request.beginDate,
                    endDate,
            )
        } else if (request.clientId != null) {
            sessionRepository.findAllByClientIdAndCreatedAtBetweenAndDeletedFalseOrderByCreatedAtDesc(
                    request.clientId,
                    request.beginDate,
                    endDate,
            )
        } else if (request.operatorId != null) {
            sessionRepository.findAllByOperatorIdAndCreatedAtBetweenAndDeletedFalseOrderByCreatedAtDesc(
                    request.operatorId,
                    request.beginDate,
                    endDate,
            )
        } else {
            sessionRepository.findAllByCreatedAtBetweenAndDeletedFalseOrderByCreatedAtDesc(request.beginDate, endDate)
        }

        if (sessionList.isEmpty()) throw EmptyListMException()

        return sessionList.map { session ->
            val messageList = messageRepository.findAllBySessionIdAndDeletedFalseOrderByCreatedAtAsc(session.id!!)
            MessageSessionResponse.toResponse(session, messageList)
        }.toList()
    }

    override fun findAllBySessionId(sessionId: Long): MessageSessionResponse {
        val session = sessionRepository.findByIdAndDeletedFalse(sessionId) ?: throw SessionNotFoundMException()

        val messageList = messageRepository.findAllBySessionIdAndDeletedFalseOrderByCreatedAtAsc(sessionId)
        if (messageList.isEmpty()) throw EmptyListMException()

        return MessageSessionResponse.toResponse(session, messageList)
    }

    override fun findAllByClientId(clientId: Long): List<MessageSessionResponse> {
        val sessionList = sessionRepository.findAllByClientIdAndDeletedFalseOrderByCreatedAtDesc(clientId)
        if (sessionList.isEmpty()) throw EmptyListMException()

        return sessionList.map { session ->
            val messageList = messageRepository.findAllBySessionIdAndDeletedFalseOrderByCreatedAtAsc(session.id!!)
            MessageSessionResponse.toResponse(session, messageList)
        }.toList()
    }

    override fun findAllByOperatorId(operatorId: Long): List<MessageSessionResponse> {
        val sessionList = sessionRepository.findAllByOperatorIdAndDeletedFalseOrderByCreatedAtDesc(operatorId)
        if (sessionList.isEmpty()) throw EmptyListMException()

        return sessionList.map { session ->
            val messageList = messageRepository.findAllBySessionIdAndDeletedFalseOrderByCreatedAtAsc(session.id!!)
            MessageSessionResponse.toResponse(session, messageList)
        }.toList()
    }
    override fun handleMessage(message: Message, chatId: Long): MessagesEntity {
        val messageType = when {
            message.text != null -> MessageType.TEXT
            message.voice != null -> MessageType.VOICE
            message.videoNote != null -> MessageType.VIDEO_NOTE
            message.audio != null -> MessageType.AUDIO
            message.video != null -> MessageType.VIDEO
            message.photo != null -> MessageType.PHOTO
            message.sticker != null -> MessageType.STICKER
            message.animation != null -> MessageType.GIF
            message.document != null -> MessageType.DOCUMENT
            message.location != null -> MessageType.LOCATION
            message.poll != null -> MessageType.POLL
            else -> MessageType.UNKNOWN
        }
        val fileId = when {
            message.voice != null -> message.voice.fileId
            message.videoNote != null -> message.videoNote.fileId
            message.audio != null -> message.audio.fileId
            message.video != null -> message.video.fileId
            message.photo != null -> message.photo.firstOrNull()?.fileId
            message.sticker != null -> message.sticker.fileId
            message.animation != null -> message.animation.fileId
            message.document != null -> message.document.fileId
            else -> null
        }

        val mediaUrl = fileId?.takeIf {
            messageType in listOf(
                    MessageType.VOICE,
                    MessageType.AUDIO,
                    MessageType.VIDEO,
                    MessageType.PHOTO,
                    MessageType.VIDEO_NOTE,
                    MessageType.STICKER,
                    MessageType.GIF,
                    MessageType.DOCUMENT
            )
        }
                ?.let { getFileUrl(it) }

        val maxFileSize = 10 * 1024 * 1024 // 10 MB
        val fileSize = fileId?.let { getFileSize(it) } ?: 0

        if (fileSize > maxFileSize) {
            val sendMessage = SendMessage().apply {
                this.chatId = message.chatId.toString()
                this.text = "Yuborish mumkin bo'lgan file'ning maksimal hajmi 10 MB"
            }
            botService.execute(sendMessage)
            throw UserBadRequestException()
        }

        val textMessage = message.text
        val replyMessageId = message.replyToMessage?.messageId
        val messageEntity = MessagesEntity(
                user = userRepository.findUserEntityByChatIdAndDeletedFalse(chatId)!!,
                text = textMessage,
                messageId = message.messageId,
                replyToMessageId = replyMessageId,
                fileId = fileId,
                mediaUrl = mediaUrl,
                messageType = messageType,
                latitude = message.location?.latitude,
                longitude = message.location?.longitude,
                session = sessionRepository.findSessionByChatIdAndStatus(chatId)!!

        )

        return messageRepository.save(messageEntity)
    }

    override fun getFileSize(fileId: String): Long {
        val file = botService.execute(GetFile(fileId))
        val fileUrl = fileApi + file.filePath
        val urlConnection = URL(fileUrl).openConnection()
        urlConnection.connect()
        return urlConnection.contentLengthLong
    }

    override fun getFileUrl(fileId: String): String {
        val getFile = GetFile(fileId)
        val file = botService.execute(getFile)
        return fileApi + file.filePath
    }


}

@Service
class SessionServiceImpl(
        private val sessionRepository: SessionRepository,
        private val userRepository: UserRepository
) : SessionService {
    @Transactional
    override fun create(request: SessionCreateRequest) {
        val user = userRepository.findByIdAndDeletedFalse(request.userId) ?: throw UserNotFoundException()
        val operator = userRepository.findUserEntityByIdAndRoleAndDeletedFalse(request.operatorId, Role.OPERATOR)
                ?: throw UserNotFoundException()
        val session = Session(
                client = user,
                operator = operator,
                status = SessionStatus.PENDING,
                rate = request.rate,
        )
        sessionRepository.save(session)
    }

    @Transactional
    override fun getFirPending(): Session? {
        return sessionRepository.findFirstPendingSession()
    }

    override fun getOne(id: Long): SessionResponse {
        return sessionRepository.findByIdAndDeletedFalse(id)?.let {
            SessionResponse.toResponse(it)
        } ?: throw SessionNotFoundMException()

    }

    override fun deleteOne(id: Long) {
        sessionRepository.trash(id) ?: throw SessionNotFoundMException()
    }

    override fun getAll(
            startTime: String?,
            endTime: String?,
            pageable: Pageable
    ): Page<SessionResponse> {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val start = startTime?.let { LocalDate.parse(it, formatter).atStartOfDay() }
        val end = endTime?.let { LocalDate.parse(it, formatter).atTime(23, 59, 59) }
        val sessions = when {
            start != null && end != null ->
                sessionRepository.findSessionByCreatedAtBetween(start, end, pageable)

            else ->
                sessionRepository.findAllNotDeletedForPageable(pageable)
        }
        return sessions.map { session ->
            SessionResponse(
                    id = session.id ?: throw IllegalStateException("Session ID cannot be null"),
                    userId = UserResponse.toResponse(session.client),
                    operatorId = UserResponse.toResponse(session.operator!!),
                    active = true
            )
        }
    }

    override fun findAllByRate(pageable: Pageable): Page<SessionResponse> {
        return sessionRepository.findAllByRateAndDeletedFalseAndActiveTrue(pageable).map {
            SessionResponse.toResponse(it)
        }
    }
}



@Service
class StatisticsServiceImpl(
    private val sessionRepository: SessionRepository,
    private val userRepository: UserRepository,
    private val statisticsMapper: StatisticsMapper
) : StatisticsService {


    override fun getTotalSessions(): TotalSessionsResponse {
        val totalSessions = sessionRepository.count()
        val totalActiveSessions = sessionRepository.countByIsActiveTrue()
        return statisticsMapper.toTotalSessionsResponse(totalSessions, totalActiveSessions)
    }

    override fun getOperatorSessionStatistics(operatorId: Long): OperatorSessionStatisticsResponse {
        val operator = userRepository.findByIdAndRoleAndDeletedFalse(operatorId, Role.OPERATOR)
            ?: throw UserNotFoundException()
        val sessions = sessionRepository.findByOperatorId(operatorId)
        return statisticsMapper.toOperatorSessionStatisticsResponse(
            operatorId = operatorId,
            operatorName = operator.fullName,
            sessions = sessions
        )
    }


    override fun getDetailedRatings(): DetailedRatingResponse {
        val allSessions = sessionRepository.findAll()
        return statisticsMapper.toDetailedRatingResponse(allSessions)
    }

    override fun getUserStatistics(): List<UserStatisticsResponse> {
        val users = userRepository.findAll()
        return users.map { user ->
            val sessions = sessionRepository.findByClientId(user.id!!)
            statisticsMapper.toUserStatisticsResponse(user, sessions)
        }
    }

    override fun getTopRatedOperators(lastMonth: Boolean, limit: Int): List<TopRatedOperatorResponse> {
        val now = LocalDateTime.now()
        val startDate = if (lastMonth) now.minusMonths(1) else LocalDateTime.MIN
        val sessions = sessionRepository.findAllSessionsByCreatedAtBetween(startDate, now)

        val operatorRatings = sessions.filter { it.operator != null }
            .groupBy { it.operator!! }
            .map { (operator, sessions) ->
                val ratings = sessions.mapNotNull { it.rate }
                val totalRatings = ratings.size.toLong()
                val averageRating = if (ratings.isNotEmpty()) ratings.average() else 0.0

                statisticsMapper.toTopRatedOperatorResponse(
                    operator = operator,
                    averageRating = averageRating,
                    totalRatings = totalRatings
                )
            }
        return operatorRatings
            .sortedByDescending { it.averageRating }
            .take(limit)
    }


    override fun getTopRatedOperator(): TopRatedOperatorResponse {
        val allSessions = sessionRepository.findAll()
        val operatorRatings = allSessions
            .filter { it.operator != null }
            .groupBy { it.operator!! }
            .map { (operator, sessions) ->
                val ratings = sessions.mapNotNull { it.rate }
                val averageRating = if (ratings.isNotEmpty()) ratings.average() else 0.0
                val totalRatings = ratings.size.toLong()
                statisticsMapper.toTopRatedOperatorResponse(operator, averageRating, totalRatings)
            }
        return operatorRatings.maxByOrNull { it.averageRating } ?: throw OperatorNotFoundException()
    }

    override fun getLowestRatedOperator(): TopRatedOperatorResponse {
        val allSessions = sessionRepository.findAll()
        val operatorRatings = allSessions
            .filter { it.operator != null }
            .groupBy { it.operator!! }
            .map { (operator, sessions) ->
                val ratings = sessions.mapNotNull { it.rate }
                val averageRating = if (ratings.isNotEmpty()) ratings.average() else 0.0
                val totalRatings = ratings.size.toLong()
                statisticsMapper.toTopRatedOperatorResponse(operator, averageRating, totalRatings)
            }
        return operatorRatings.minByOrNull { it.averageRating } ?: throw OperatorNotFoundException()
    }

    override fun getOperatorAverageRatings(): Map<String, Double> {
        val allSessions = sessionRepository.findAll()
        val averageRatings = allSessions
            .filter { it.operator != null }
            .groupBy { it.operator!! }
            .mapValues { (_, sessions) ->
                val ratings = sessions.mapNotNull { it.rate }
                if (ratings.isNotEmpty()) ratings.average() else 0.0
            }
        return averageRatings.mapKeys { it.key.fullName }
    }
}

