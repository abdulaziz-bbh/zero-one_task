package com.bbhgroup.zeroone_task

import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

@Component
class DataLoader(
    private val userRepository: UserRepository
) : CommandLineRunner {

    override fun run(vararg args: String?) {
        loadAdmin()
    }

    private fun loadAdmin() {
        if (userRepository.countByRole(Role.ADMIN) == 0L) {
            val admin = UserEntity(
                fullName = "John Doe",
                phoneNumber = "905969167",
                chatId = 999999999L,
                role = Role.ADMIN,
                language = setOf(Languages.ENG)
            )
            userRepository.save(admin)
            println("Admin user loaded")
        }
    }
}