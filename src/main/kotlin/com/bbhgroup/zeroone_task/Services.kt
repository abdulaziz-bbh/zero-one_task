package com.bbhgroup.zeroone_task

import org.springframework.stereotype.Service


interface UserService{}
interface MessageService{}
interface SessionService{}
interface QueueService{}
interface RatingService{}

@Service
class UserServiceImpl(private val userRepository: UserRepository):UserService{

}

@Service
class QueueServiceImpl(private val queueRepository: QueueRepository):QueueService{}

@Service
class MessageServiceImpl(private val messageRepository: MessageRepository):MessageService{
}

@Service
class SessionServiceImpl(private val sessionRepository: SessionRepository):SessionService{}

@Service
class RatingServiceImpl(private val ratingRepository: RatingRepository):RatingService{}


