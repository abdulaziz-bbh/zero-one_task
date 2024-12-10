package com.bbhgroup.zeroone_task

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import java.util.*

@Component
class BotService(
    private val userRepository: UserRepository,
    private val messageSource: ResourceBundleMessageSource
) : TelegramLongPollingBot() {

    private var isLanguageSelected = false

    override fun getBotUsername(): String = "@ZeroOneOperator_bot"
    override fun getBotToken(): String = "7668006662:AAFGspD8yJTB5njgXCjSYAmBQZvdDnaBss8"

    override fun onUpdateReceived(update: Update?) {
        if (update?.hasMessage() == true && update.message.text == "/start") {
            val chatId = update.message.chatId
            sendLanguageButtons(chatId)
        } else if (update?.hasCallbackQuery() == true) {
            val chatId = update.callbackQuery.message.chatId
            val local = update.callbackQuery.data
            isLanguageSelected = true
            sendContactRequest(chatId, local )
        } else if (update?.hasMessage() == true && isLanguageSelected && update.message.contact != null) {

            isLanguageSelected = false
            val number = update.message.contact.phoneNumber
            val name = update.message.contact.firstName
            sendConfirmationMessage(update.message.chatId)
            removeContactButton(update.message.chatId)
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
                        InlineKeyboardButton("🇬🇧 English").apply { callbackData = Languages.ENG.key }
                    )
                )
            }
        }
        execute(message)
    }

    private fun sendContactRequest(chatId: Long, local: String) {
        val message = SendMessage().apply {
            this.chatId = chatId.toString()
            text = getMessage("SHARE_CONTACT", local)!!
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
            text = "Kontakt ma'lumotlaringiz muvaffaqiyatli ulashildi."
        }
        execute(message)
    }

    private fun removeContactButton(chatId: Long) {
        val message = SendMessage().apply {

            this.chatId = chatId.toString()
            text = "Rahmat! Sizning kontakt ma'lumotlaringiz muvaffaqiyatli ulashildi."
            replyMarkup = ReplyKeyboardRemove().apply {
                removeKeyboard = true
            }
        }
        execute(message)
    }

    fun getMessage(code: String, local: String): String?{
        return messageSource.getMessage(code, null, Locale(local))
    }
}
