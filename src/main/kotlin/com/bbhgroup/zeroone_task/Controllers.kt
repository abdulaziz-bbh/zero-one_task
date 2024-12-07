package com.bbhgroup.zeroone_task

import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton

@Component
class BotController : TelegramLongPollingBot() {

    override fun getBotUsername(): String = "@ZeroOneOperator_bot"
    override fun getBotToken(): String = "7668006662:AAFGspD8yJTB5njgXCjSYAmBQZvdDnaBss8"

    override fun onUpdateReceived(update: Update?) {
        if (update?.hasMessage() == true && update.message.text == "/start") {
            val chatId = update.message.chatId
            sendLanguageButtons(chatId) // 1-qadam: Tugmalarni yuborish
        } else if (update?.hasCallbackQuery() == true) {
            val chatId = update.callbackQuery.message.chatId
            val selectedLanguage = update.callbackQuery.data // 2-qadam: Foydalanuvchi tanlagan til
            println("languege: $selectedLanguage chatId: $chatId")
            val sendMessage = SendMessage()
            sendMessage.chatId = chatId.toString()
            sendMessage.text = selectedLanguage
            execute(sendMessage)
        }
    }
    private fun sendLanguageButtons(chatId: Long) {
        val message = SendMessage().apply {
            this.chatId = chatId.toString()
            text = "Tilni tanlang:"
            replyMarkup = InlineKeyboardMarkup().apply {
                keyboard = listOf(
                    listOf(
                        InlineKeyboardButton("\uD83C\uDDF7\uD83C\uDDFA Русский язык").apply { callbackData = "ru" },
                        InlineKeyboardButton("\uD83C\uDFF4\uDB40\uDC67\uDB40\uDC62\uDB40\uDC65\uDB40\uDC6E\uDB40\uDC67\uDB40\uDC7F English").apply { callbackData = "en" },
                        InlineKeyboardButton("\uD83C\uDDFA\uD83C\uDDFF O'zbek tili").apply { callbackData = "uz" }
                    )
                )
            }
        }
        execute(message)
    }

}