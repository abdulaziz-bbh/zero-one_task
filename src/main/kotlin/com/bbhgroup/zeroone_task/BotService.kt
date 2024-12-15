package com.bbhgroup.zeroone_task

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Lazy
import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.*
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.objects.InputFile
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
    private val userService: UserService
) : TelegramLongPollingBot() {

    private var language: String? = null
    private var languageCodes: MutableMap<Long, String> = mutableMapOf()
    var botSteps = BotSteps.CONNECT_OPERATOR

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
                        writeToOperator(update, chatId)
                    }
                    if (role == Role.OPERATOR) {
                        sendStartWorkButton(chatId)
                            sendPendingMessage(chatId)
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
               botSteps = BotSteps.CONNECT_OPERATOR
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

         sessionRepository.findByChatIdAndIsActiveTrue(client.chatId)
            ?: createSessionForClient(client)

        val connectMessage = SendMessage().apply {
            this.chatId = chatId.toString()
            text = getMessage(MessageKeys.WRITE_TO_OPERATOR.name, languageCodes[chatId]!!)
//            replyMarkup = ReplyKeyboardMarkup().apply {
//                keyboard = mutableListOf(
//                    KeyboardRow().apply {
//                        add(KeyboardButton("Chatni yakunlash"))
//                        botSteps = BotSteps.END_CHAT
//                    }
//                )
//                resizeKeyboard = true
//                oneTimeKeyboard = true
//            }
        }
        execute(connectMessage)
    }

    private fun endChat(chatId: Long) {
        val client = userRepository.findUserEntityByChatIdAndDeletedFalse(chatId)
            ?: throw RuntimeException("Foydalanuvchi topilmadi.")

        val activeSession = sessionRepository.findByChatIdAndIsActiveTrue(client.chatId)
            ?: throw RuntimeException("Faol session topilmadi.")

        activeSession.status =SessionStatus.COMPLETED
        sessionRepository.save(activeSession)

        val endMessage = SendMessage().apply {
            this.chatId = chatId.toString()
            text = "Operator bilan bog'lanish yakunlandi."
        }
        execute(endMessage)
    }

    private fun sendConnectOperatorButton(chatId: Long) {
        val message = SendMessage().apply {
            this.chatId = chatId.toString()
            text = getMessage(MessageKeys.TEXT_CONNECT_BUTTON_TO_OPERATOR.name, languageCodes[chatId]!!)
            replyMarkup = InlineKeyboardMarkup().apply {
                keyboard = listOf(
                    listOf(
                        InlineKeyboardButton(
                            getMessage(MessageKeys.CONNECT_BUTTON_TO_OPERATOR.name, languageCodes[chatId]!!)).apply {
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
    fun workingOfOperator(update: Update, chatId: Long) {
        val language = userService.getLanguages(chatId)
        val user = userRepository.findUserEntityByChatIdAndDeletedFalse(chatId)!! //todo exceptin yozish kerak
        if (update.hasCallbackQuery() && update.callbackQuery.data == "START_WORK"){
            user.status = Status.BUSY
            userRepository.save(user)
            sendStartWorkButton(chatId)
        }

    }
    private fun sendPendingMessage(operatorChatId: Long){
        val session = sessionRepository.findFirstPendingSession()!!
        session.status = SessionStatus.PROCESSING
        sendSessionMessagesToChat(session.id!!, operatorChatId)

    }
    private fun sendStartWorkButton(chatId: Long) {
        val message = SendMessage().apply {
        this.chatId = chatId.toString()
        text = getMessage(MessageKeys.START_WORK.name, languageCodes[chatId]!!)
        replyMarkup = InlineKeyboardMarkup().apply {
            keyboard = listOf(
                listOf(
                    InlineKeyboardButton(
                        getMessage(MessageKeys.START_WORK_BUTTON.name, languageCodes[chatId]!!)).apply {
                        callbackData = "START_WORK"
                    }
                )
            )
        }
    }
        execute(message)}


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
    fun sendMessageToChat(message: MessagesEntity, targetChatId: Long) {
        try {
            when {
                // Matnli xabarni yuborish
                message.text != null -> {
                    val sendMessage = SendMessage()
                    sendMessage.chatId = targetChatId.toString()
                    sendMessage.text = message.text!!
                    message.replyToMessageId?.let { sendMessage.replyToMessageId = it }
                    execute(sendMessage)
                }

                // Media fayl yuborish
                message.fileId != null -> when (message.messageType) {
                    MessageType.PHOTO -> {
                        val sendPhoto = SendPhoto()
                        sendPhoto.chatId = targetChatId.toString()
                        sendPhoto.photo = InputFile(message.fileId)
                        message.replyToMessageId?.let { sendPhoto.replyToMessageId = it }
                        execute(sendPhoto)
                    }
                    MessageType.VIDEO -> {
                        val sendVideo = SendVideo()
                        sendVideo.chatId = targetChatId.toString()
                        sendVideo.video = InputFile(message.fileId)
                        message.replyToMessageId?.let { sendVideo.replyToMessageId = it }
                        execute(sendVideo)
                    }
                    MessageType.VOICE -> {
                        val sendVoice = SendVoice()
                        sendVoice.chatId = targetChatId.toString()
                        sendVoice.voice = InputFile(message.fileId)
                        message.replyToMessageId?.let { sendVoice.replyToMessageId = it }
                        execute(sendVoice)
                    }

                    else -> println("Unsupported media type: ${message.messageType}")
                }

                // Lokatsiya yuborish
                message.latitude != null && message.longitude != null -> {
                    val sendLocation = SendLocation()
                    sendLocation.chatId = targetChatId.toString()
                    sendLocation.latitude = message.latitude!!
                    sendLocation.longitude = message.longitude!!
                    message.replyToMessageId?.let { sendLocation.replyToMessageId = it }
                    execute(sendLocation)
                }

                else -> println("Message content is empty: ${message.messageId}")
            }
        } catch (e: TelegramApiException) {
            println("Error while sending message ${message.messageId}: ${e.message}")
        }
    }
    fun sendSessionMessagesToChat(sessionId: Long, targetChatId: Long) {
        val messages = messageRepository.findAllBySessionIdOrderByCreatedAtAsc(sessionId)
        for (message in messages) {
            sendMessageToChat(message, targetChatId)
        }
    }




}