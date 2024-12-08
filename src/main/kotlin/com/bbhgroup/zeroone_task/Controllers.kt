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
            var sendMessage = SendMessage()
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
                        InlineKeyboardButton("🇷🇺 Русский").apply { callbackData = Languages.RU.key },
                        InlineKeyboardButton("🇺🇿 O'zbek").apply { callbackData = Languages.UZ.key},
                        InlineKeyboardButton("🇬🇧 English").apply { callbackData= Languages.ENG.key }           )
                )
            }
        }
        execute(message)
    }

}