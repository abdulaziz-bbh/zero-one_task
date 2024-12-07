package com.bbhgroup.zeroone_task

import org.springframework.stereotype.Service


interface UserService
@Service
class UserServiceImpl(private val userRepository: UserRepository):UserService{

}

