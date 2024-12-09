package com.bbhgroup.zeroone_task

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession


@SpringBootApplication
@EnableJpaRepositories(repositoryBaseClass = BaseRepositoryImpl::class)
class ZeroOneTaskApplication

fun main(args: Array<String>) {
    val context = runApplication<ZeroOneTaskApplication>(*args)
    val telegramBot = context.getBean(BotService::class.java)

    try {
        val botsApi = TelegramBotsApi(DefaultBotSession::class.java)
        botsApi.registerBot(telegramBot)
    } catch (e: TelegramApiException) {
        e.printStackTrace()
    }
}

