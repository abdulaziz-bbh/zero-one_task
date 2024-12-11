package com.bbhgroup.zeroone_task

import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.GetFile
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import java.net.URL
import java.util.*

@Component
class BotService(
    private val userRepository: UserRepository,
    private val messageRepository: MessageRepository,
    private val sessionRepository: SessionRepository,
    private val messageSource: ResourceBundleMessageSource
) : TelegramLongPollingBot() {

    private var isLanguageSelected = false
    private var language: String? = null
    var botSteps = BotSteps.START

    override fun getBotUsername(): String = "@ZeroOneOperator_bot"
    override fun getBotToken(): String = "7668006662:AAFGspD8yJTB5njgXCjSYAmBQZvdDnaBss8"

    override fun onUpdateReceived(update: Update?) {
        if (update == null) return

        val chatId = when {
            update.hasMessage() -> update.message.chatId
            update.hasCallbackQuery() -> update.callbackQuery.message.chatId
            else -> null
        }

        if (chatId != null) {
            if (userRepository.existsByChatId(chatId)) {
                handleExistingUser(update, chatId)
            } else {
                registerUser(update)
            }
        }
    }
    private fun registerUser(update: Update?){
        val chatId: Long?
        val name: String?
        val number: String?

        if (update?.hasMessage() == true && botSteps == BotSteps.START) {
            chatId = update.message.chatId
            sendLanguageButtons(chatId)
            botSteps = BotSteps.SELECT_LANGUAGE
            return
        }
        if (update?.hasCallbackQuery() == true && botSteps == BotSteps.SELECT_LANGUAGE) {
            chatId = update.callbackQuery.message.chatId
            language = update.callbackQuery.data.toString()
            isLanguageSelected = true
            sendContactRequest(chatId, language!!)
            botSteps = BotSteps.SHARE_CONTACT
            return
        }

        if (update?.hasMessage() == true && isLanguageSelected && update.message.contact != null && botSteps == BotSteps.SHARE_CONTACT) {
            chatId = update.message.chatId
            number = update.message.contact.phoneNumber
            name = update.message.contact.firstName
            isLanguageSelected = false
            sendConfirmationMessage(chatId)
            removeContactButton(chatId)
            saveUser(chatId, name, language!!.uppercase(), number)
            sendConnectOperatorButton(chatId)
            botSteps = BotSteps.CONNECT_OPERATOR
            return
        }
    }
    private fun saveUser(chatId: Long, fullName: String?, language: String?, phone: String?) {
        val user = UserEntity(
            chatId = chatId,
            fullName = fullName!!,
            language = setOf(Languages.valueOf(language!!)),
            phoneNumber = phone!!
        )
        userRepository.save(user)
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
    private fun sendContactRequest(chatId: Long, local: String) {
        val message = SendMessage().apply {
            this.chatId = chatId.toString()
            text = getMessage("SHARE_CONTACT", local)
            replyMarkup = createContactButton()
        }
        execute(message)
    }
    private fun createContactButton(): ReplyKeyboardMarkup {
        val row = KeyboardRow().apply {
            add(KeyboardButton("Share Contact").apply {
                requestContact = true
            })
        }
        return ReplyKeyboardMarkup().apply {
            keyboard = mutableListOf(row)
            resizeKeyboard = true
            oneTimeKeyboard = true
        }
    }
    private fun sendConfirmationMessage(chatId: Long) {
        val message = SendMessage().apply {
            this.chatId = chatId.toString()
            text = getMessage("SUCCESS_SHARE_CONTACT", language!!)
        }
        execute(message)
    }
    private fun removeContactButton(chatId: Long) {
        val message = SendMessage().apply {

            this.chatId = chatId.toString()
            text = "..."
            replyMarkup = ReplyKeyboardRemove().apply {
                removeKeyboard = true
            }
        }
        execute(message)
    }
    private fun getMessage(code: String, local: String): String {
        return messageSource.getMessage(code, null, Locale(local))
    }
    private fun handleExistingUser(update: Update, chatId: Long) {
        val messageText:String? = update.message?.text
       if (botSteps == BotSteps.CONNECT_OPERATOR){
           startOperatorCommunication(chatId)
           botSteps = BotSteps.SENDING_MESSAGES
       }else if (botSteps == BotSteps.SENDING_MESSAGES){
           if (update.hasMessage()) {
               handleMessage(update.message, chatId)
           }
       }else if (messageText == "Chatni yakunlash") {
            endChat(chatId)
            return
        }
    }

    private fun startOperatorCommunication(chatId: Long) {
        val client = userRepository.findUserEntityByChatIdAndDeletedFalse(chatId)
            ?: throw RuntimeException("Foydalanuvchi topilmadi.")

         sessionRepository.findByChatIdAndIsActiveTrue(client.chatId)
            ?: createSessionForClient(client)

        val connectMessage = SendMessage().apply {
            this.chatId = chatId.toString()
            text = "Operator bilan muloqot boshlandi. Xabaringizni yozing."
            replyMarkup = ReplyKeyboardMarkup().apply {
                keyboard = mutableListOf(
                    KeyboardRow().apply {
                        add(KeyboardButton("Chatni yakunlash"))
                        botSteps = BotSteps.END_CHAT
                    }
                )
                resizeKeyboard = true
                oneTimeKeyboard = true
            }
        }
        execute(connectMessage)
    }

    private fun endChat(chatId: Long) {
        val client = userRepository.findUserEntityByChatIdAndDeletedFalse(chatId)
            ?: throw RuntimeException("Foydalanuvchi topilmadi.")

        val activeSession = sessionRepository.findByChatIdAndIsActiveTrue(client.chatId)
            ?: throw RuntimeException("Faol session topilmadi.")

        activeSession.isActive = false
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
            text = "Operator bilan bog'lanish uchun quyidagi tugmani bosing:"
            replyMarkup = InlineKeyboardMarkup().apply {
                keyboard = listOf(
                    listOf(
                        InlineKeyboardButton("Operator bilan bog'lanish").apply {
                            callbackData = "CONNECT_OPERATOR"
                        }
                    )
                )
            }
        }
        execute(message)
    }
    fun handleMessage(message: Message, chatId: Long) {
        val messageType = when {
            message.text != null -> MessageType.TEXT
            message.voice != null -> MessageType.VOICE
            message.videoNote != null -> MessageType.VIDEO_NOTE
            message.audio != null -> MessageType.AUDIO
            message.video != null -> MessageType.VIDEO
            message.photo != null -> MessageType.PHOTO
            message.sticker != null -> MessageType.STICKER
            message.animation != null -> MessageType.GIF
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
            else -> null
        }

        val mediaUrl = if (messageType in listOf(MessageType.VOICE, MessageType.AUDIO, MessageType.VIDEO, MessageType.PHOTO)) {
            getFileUrl(fileId!!)
        } else {
            null
        }

        val maxFileSize = 10 * 1024 * 1024 // 10 MB
        val fileSize = fileId?.let { getFileSize(it) } ?: 0

        if (fileSize > maxFileSize) {
            val sendMessage = SendMessage().apply {
                this.chatId = message.chatId.toString()
                this.text = "Yuborish mumkin bo'lgan file'ning maksimal hajmi 10 MB"
            }
            execute(sendMessage)
            return
        }

        val textMessage = message.text
        val messageEntity = MessagesEntity(
            user = userRepository.findUserEntityByChatIdAndDeletedFalse(chatId)!!,
            text = textMessage,
            fileId = fileId,
            mediaUrl = mediaUrl,
            messageType = messageType,
            session = sessionRepository.findByChatIdAndIsActiveTrue(chatId)!!
        )

        messageRepository.save(messageEntity)
    }


    fun getFileSize(fileId: String): Long {
        val file = execute(GetFile(fileId))
        val fileUrl = "https://api.telegram.org/file/bot${botToken}/${file.filePath}"
        val urlConnection = URL(fileUrl).openConnection()
        urlConnection.connect()
        return urlConnection.contentLengthLong
    }
    fun getFileUrl(fileId: String): String {
        val getFile = GetFile(fileId)
        val file = execute(getFile)
        return "https://api.telegram.org/file/bot${botToken}/${file.filePath}"
    }

    fun createSessionForClient(client: UserEntity): Session {
        val session = Session(
            client = client,
            isActive = true,
            operator = null
        )
        return sessionRepository.save(session)
    }


}
