package com.bbhgroup.zeroone_task

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootApplication
@EnableJpaRepositories(repositoryBaseClass = BaseRepositoryImpl::class)
class ZeroOneTaskApplication

fun main(args: Array<String>) {
    runApplication<ZeroOneTaskApplication>(*args)
}
