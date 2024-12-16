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
        val adminPhoneNumber = "905969167"
        if (userRepository.countByRole(Role.ADMIN) == 0L && userRepository.findByPhoneNumberAndDeletedFalse(adminPhoneNumber) == null) {
            val admin = UserEntity(
                fullName = "John Doe",
                phoneNumber = adminPhoneNumber,
                chatId = 999999999L,
                role = Role.ADMIN,
                language = mutableSetOf(Languages.EN)
            )
            userRepository.save(admin)
            println("Admin user loaded")
        } else {
            println("Admin user already exists")
        }
    }
}
