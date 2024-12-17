package com.bbhgroup.zeroone_task

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Lazy
import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.CopyMessage
import org.telegram.telegrambots.meta.api.methods.send.*
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import java.util.*

@Component
class BotService(
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository,
    private val messageSource: ResourceBundleMessageSource,
    private val messageRepository: MessageRepository,
    @Lazy private val messageService: MessageService,
    private val userService: UserService,
    private val sessionService: SessionService,
) : TelegramLongPollingBot() {

    private var language: String? = null
    private var languageCodes: MutableMap<Long, String> = mutableMapOf()

    @Value("\${bot.username}")
    lateinit var username:String

    @Value("\${bot.token}")
    lateinit var token:String

    override fun getBotUsername(): String = username
    override fun getBotToken(): String = token

    override fun onUpdateReceived(update: Update?) {
        if (update == null) return

        val chatId = when {
            update.hasMessage() -> update.message.chatId
            update.hasCallbackQuery() -> update.callbackQuery.message.chatId
            else -> null
        }

        if (chatId != null) {
            if (userService.existsByChatId(chatId)) {
                val user:UserEntity = userRepository.findUserEntityByChatIdAndDeletedFalse(chatId)!!
                user.run {
                    if (role == Role.USER) {
                        val session = sessionRepository.findProcessingSessionsByClientId(chatId)
                        if (update.hasCallbackQuery() && botSteps == BotSteps.END_CHAT){
                            selectRateForSession(chatId, update.callbackQuery.data.toInt())
                            deleteMessage(chatId, update.callbackQuery.message.messageId)
                        }
                        if (update.hasCallbackQuery()  && botSteps == BotSteps.START) {
                            writeToOperator(update, chatId)
                        }
                        if (update.hasMessage() && botSteps == BotSteps.SENDING_MESSAGES && session == null) {
                            writeToOperator(update, chatId)
                        }
                        if (update.hasMessage() && botSteps == BotSteps.START) {
                            sendConnectOperatorButton(chatId)
                        }
                        if (update.hasMessage() && botSteps == BotSteps.SENDING_MESSAGES && session != null){
                            sendMessageToOperator(chatId, update.message)
                        }
                    }
                    if (role == Role.OPERATOR) {
                        if (update.hasMessage() && update.message.text == "/start"){
                            sendStartWorkButton(chatId)
                            deleteMessage(chatId, update.message.messageId)
                        }

                        if (update.hasCallbackQuery() && update.callbackQuery.data =="START_WORK") {
                            deleteMessage(chatId, update.callbackQuery.message.messageId)
                            sendPendingMessage(chatId)
                        }
                        if (update.hasMessage() && update.message.text == getMessage(MessageKeys.END_CHAT.name, userService.getLanguages(chatId))){
                            endChat(chatId)
                            deleteMessage(chatId, update.message.messageId)
                        }
                        if (update.hasMessage() && update.message.text == getMessage(MessageKeys.END_WORK.name, userService.getLanguages(chatId))){
                            endWork(chatId)
                            deleteMessage(chatId, update.message.messageId)
                        }
                        if (update.hasMessage() && status == Status.BUSY) {
                            sendMessageToClient(chatId, update.message)
                        }
                    }
                }
            } else {
                registerUser(update)
            }
        }
    }

    private fun registerUser(update: Update?){
        val chatId: Long?
        val name: String?
        val number: String?

        if (update?.hasMessage() == true && update.message.text == "/start") {
            chatId = update.message.chatId
            sendLanguageButtons(chatId)
            return
        }
        if (update?.hasCallbackQuery() == true) {
            val messageId = update.callbackQuery.message.messageId
            chatId = update.callbackQuery.message.chatId
            language = update.callbackQuery.data.toString()
            if (languageCodes[chatId] == null) {
                languageCodes[chatId] = update.callbackQuery.data.toString()
            }
            deleteMessage(chatId, messageId)
            sendContactRequest(chatId)
            return
        }

       if (update?.hasMessage() == true  && update.message.contact != null) {
           val senderId = update.message.contact.userId
           val fromId = update.message.from.id
           if (senderId == fromId) {
               chatId = update.message.chatId
               number = update.message.contact.phoneNumber
               name = update.message.contact.firstName
               removeContactButton(chatId)
               userService.saveUser(chatId, name, language!!.uppercase(), number)
               sendConnectOperatorButton(chatId)
               return
           }else{
               sendTextMessage(
                   update.message.chatId,
                   getMessage(MessageKeys.SHARE_OTHER_CONTACT.name, languageCodes[update.message.chatId]!!)
               )
           }
        }
    }

    private fun sendLanguageButtons(chatId: Long) {
        val message = SendMessage().apply {
            this.chatId = chatId.toString()
            text = "Tilni tanlang/Choose language/Выберите язык:"
            replyMarkup = InlineKeyboardMarkup().apply {
                keyboard = listOf(
                    listOf(
                        InlineKeyboardButton("🇷🇺 Русский").apply { callbackData = Languages.RU.key },
                        InlineKeyboardButton("🇺🇿 O'zbek").apply { callbackData = Languages.UZ.key },
                        InlineKeyboardButton("🇬🇧 English").apply { callbackData = Languages.EN.key }
                    )
                )
            }
        }
        execute(message)
    }

    private fun sendContactRequest(chatId: Long) {
        val message = SendMessage().apply {
            this.chatId = chatId.toString()
            text = getMessage(MessageKeys.SHARE_CONTACT.name, languageCodes[chatId]!!)
            replyMarkup = createContactButton(chatId)
        }
        execute(message)
    }

    private fun createContactButton(chatId: Long): ReplyKeyboardMarkup {
        val row = KeyboardRow().apply {
            add(KeyboardButton(getMessage(MessageKeys.SHARE_CONTACT_BUTTON.name, languageCodes[chatId]!!)).apply {
                requestContact = true
            })
        }
        return ReplyKeyboardMarkup().apply {
            keyboard = mutableListOf(row)
            resizeKeyboard = true
            oneTimeKeyboard = true
        }
    }

    private fun removeContactButton(chatId: Long) {
        val message = SendMessage().apply {

            this.chatId = chatId.toString()
            text = getMessage(MessageKeys.SUCCESS_SHARE_CONTACT.name, languageCodes[chatId]!!)
            replyMarkup = ReplyKeyboardRemove().apply {
                removeKeyboard = true
            }
        }
        execute(message)
    }

    private fun getMessage(code: String, local: String): String {
        return messageSource.getMessage(code, null, Locale(local))
    }

    private fun writeToOperator(update: Update, chatId: Long) {
        val user = userRepository.findUserEntityByChatIdAndDeletedFalse(chatId)!!
       if (user.botSteps == BotSteps.START && update.callbackQuery.data == "CONNECT_OPERATOR"){
           deleteMessage(chatId, update.callbackQuery.message.messageId)
           startOperatorCommunication(chatId)
           user.botSteps = BotSteps.SENDING_MESSAGES
           userRepository.save(user)
       }else if (user.botSteps == BotSteps.SENDING_MESSAGES){
           if (update.hasMessage()) {
               messageService.handleMessage(update.message, chatId)
           }
       }
    }

    private fun startOperatorCommunication(chatId: Long) {
        val client = userRepository.findUserEntityByChatIdAndDeletedFalse(chatId)
            ?: throw RuntimeException("Foydalanuvchi topilmadi.")


        val connectMessage = SendMessage().apply {
            this.chatId = chatId.toString()
            text = if (languageCodes[chatId] == null) {
                getMessage(MessageKeys.WRITE_TO_OPERATOR.name,userService.getLanguages(chatId))
            }else {
                getMessage(MessageKeys.WRITE_TO_OPERATOR.name, languageCodes[chatId]!!)
            }        }
        sessionRepository.findByChatIdAndIsActiveTrue(client.chatId)
            ?: createSessionForClient(client)
        execute(connectMessage)
    }

    private fun sendConnectOperatorButton(chatId: Long) {
        val message = SendMessage().apply {
            this.chatId = chatId.toString()
            text = if (languageCodes[chatId] == null) {
                getMessage(MessageKeys.TEXT_CONNECT_BUTTON_TO_OPERATOR.name,userService.getLanguages(chatId))
            }else {
                getMessage(MessageKeys.TEXT_CONNECT_BUTTON_TO_OPERATOR.name, languageCodes[chatId]!!)
            }
            replyMarkup = InlineKeyboardMarkup().apply {
                keyboard = listOf(
                    listOf(
                        InlineKeyboardButton(
                            if (languageCodes[chatId] == null) {
                                getMessage(MessageKeys.CONNECT_BUTTON_TO_OPERATOR.name,userService.getLanguages(chatId))
                            }else {
                                getMessage(MessageKeys.CONNECT_BUTTON_TO_OPERATOR.name, languageCodes[chatId]!!)
                            }).apply {
                            callbackData = "CONNECT_OPERATOR"
                        }
                    )
                )
            }
        }
        execute(message)
    }

    fun createSessionForClient(client: UserEntity): Session {
        val session = Session(
            client = client,
            status = SessionStatus.PENDING,
            operator = null
        )
        return sessionRepository.save(session)
    }
    private fun sendPendingMessage(operatorChatId: Long){
        val session = sessionService.getFirPending()
        val operator = userRepository.findUserEntityByChatIdAndDeletedFalse(operatorChatId)!!
        val oLang = operator.language
        val cLang = session?.client?.language
        val isMatch = cLang?.let { oLang.containsAll(it) }
        if (session != null && isMatch!!){
            operator.status = Status.BUSY
            session.status = SessionStatus.PROCESSING
            session.operator = operator
            sessionRepository.save(session)
            userRepository.save(operator)
            sendEndChatOrWorkButton(operator.chatId, session.client.chatId)
            sendSessionMessagesToChat(session.id!!, operatorChatId)
        }else{
            sendTextMessage(operatorChatId,getMessage(
                MessageKeys.NOT_FOUND_PENDING_CLIENT.name,
                userService.getLanguages(operatorChatId)))
            sendStartWorkButton(operatorChatId)
        }
    }

    private fun sendEndChatOrWorkButton(operatorChatId: Long, clientChatId: Long){
        val sendMessage = SendMessage().apply {
            this.chatId = operatorChatId.toString()
            this.replyMarkup = createEndChatButtons(operatorChatId)
            this.text = "chat id : $clientChatId"
        }
        execute(sendMessage)
    }
    private fun createEndChatButtons(chatId: Long): ReplyKeyboardMarkup {
        val row = KeyboardRow().apply {
            add(KeyboardButton(getMessage(MessageKeys.END_CHAT.name, userService.getLanguages(chatId))))
            add(KeyboardButton(getMessage(MessageKeys.END_WORK.name, userService.getLanguages(chatId))))
        }
        return ReplyKeyboardMarkup().apply {
            keyboard = mutableListOf(row)
            resizeKeyboard = true
            oneTimeKeyboard = true
        }
    }

    fun sendSessionMessagesToChat(sessionId: Long, toChatId: Long) {
        val messages = messageRepository.findAllBySessionIdOrderByCreatedAtAsc(sessionId)
        for (message in messages) {
            copyMessage(message.user.chatId, message.messageId!!, toChatId, message.replyToMessageId)
        }
    }
    fun sendMessageToClient(operatorChatId: Long, message: Message) {
        val session = sessionRepository.findProcessingSessionsByOperatorId(operatorChatId)!!
        val clientChatId = session.client.chatId
        val replyToMessageId: Int? = message.replyToMessage?.messageId
        copyMessage(operatorChatId, message.messageId, clientChatId,replyToMessageId)
    }
    fun sendMessageToOperator(clientChatId: Long, message: Message) {
        val session = sessionRepository.findProcessingSessionsByClientId(clientChatId)!!
        val operatorChatId = session.operator?.chatId
        val replyToMessageId: Int? = message.replyToMessage?.messageId
        copyMessage(clientChatId, message.messageId, operatorChatId!!, replyToMessageId)
    }

    private var replyToMessageIdMap: MutableMap<Int, Int> = mutableMapOf()
    fun copyMessage(fromChatId: Long, messageId: Int, toChatId: Long, replyToMessageId: Int?) {
        try {
            val copyMessage = CopyMessage().apply {
                this.fromChatId = fromChatId.toString()
                this.chatId = toChatId.toString()
                this.messageId = messageId
                if (replyToMessageId != null ) {
                    this.replyToMessageId = replyToMessageIdMap[replyToMessageId]
                }
        }
          val newMessageId =  execute(copyMessage)
            replyToMessageIdMap[messageId] = newMessageId.messageId?.toInt()!!
        } catch (e: TelegramApiException) {
            println("Message: ${e.message}")
        }
    }

    private fun sendStartWorkButton(chatId: Long) {
        val message = SendMessage().apply {
            this.chatId = chatId.toString()
            text = getMessage(MessageKeys.START_WORK.name,userService.getLanguages(chatId))
            replyMarkup = InlineKeyboardMarkup().apply {
                keyboard = listOf(
                    listOf(
                        InlineKeyboardButton(
                            getMessage(MessageKeys.START_WORK_BUTTON.name, userService.getLanguages(chatId))).apply {
                            callbackData = "START_WORK"
                        }
                    )
                )
            }
        }
        execute(message)
    }

    private fun endChat(operatorChatId: Long){
        val operator = userRepository.findUserEntityByChatIdAndDeletedFalse(operatorChatId)!!
        operator.status = Status.FREE
        userRepository.save(operator)
        val session = sessionRepository.findProcessingSessionsByOperatorId(operatorChatId)!!
        session.status = SessionStatus.COMPLETED
        sessionRepository.save(session)
        val client = userRepository.findUserEntityByChatIdAndDeletedFalse(session.client.chatId)!!
        client.botSteps = BotSteps.END_CHAT
        userRepository.save(client)
        sendNumberButtons(client.chatId)
        sendPendingMessage(operatorChatId)
    }

    private fun endWork(operatorChatId: Long){
        val operator = userRepository.findUserEntityByChatIdAndDeletedFalse(operatorChatId)!!
        operator.status = Status.NOT_WORKING
        userRepository.save(operator)
        val session = sessionRepository.findProcessingSessionsByOperatorId(operatorChatId)!!
        session.status = SessionStatus.COMPLETED
        sessionRepository.save(session)
        val client = userRepository.findUserEntityByChatIdAndDeletedFalse(session.client.chatId)!!
        client.botSteps = BotSteps.END_CHAT
        userRepository.save(client)
        sendNumberButtons(session.client.chatId)
        sendStartWorkButton(operatorChatId)
    }

    private fun selectRateForSession(clientChatId: Long, rate: Int) {
        val session = sessionRepository.findLastCompletedSession(clientChatId)!!
        session.rate = rate
        sessionRepository.save(session)
        val client = userRepository.findUserEntityByChatIdAndDeletedFalse(clientChatId)!!
        client.botSteps = BotSteps.START
        userRepository.save(client)
        sendConnectOperatorButton(clientChatId)
    }

    private fun sendNumberButtons(chatId: Long) {
        val message = SendMessage().apply {
            this.chatId = chatId.toString()
            text = getMessage(MessageKeys.SEND_RATE_VALUE.name, userService.getLanguages(chatId))
            replyMarkup = InlineKeyboardMarkup().apply {
                keyboard = listOf(
                    (1..5).map { number ->
                        InlineKeyboardButton(number.toString()).apply {
                            callbackData = number.toString()
                        }
                    }
                )
            }
        }
        execute(message)
    }

    private fun deleteMessage(chatId: Long, messageId: Int) {
        val deletedMessage = DeleteMessage().apply {
            this.messageId = messageId
            this.chatId = chatId.toString()
        }
        execute(deletedMessage)
    }

    private fun sendTextMessage(chatId: Long, messageText: String) {
        val message = SendMessage().apply {
            this.chatId = chatId.toString()
            this.text = messageText
        }
        execute(message)
    }
}
