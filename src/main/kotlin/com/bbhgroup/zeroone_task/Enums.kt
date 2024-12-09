package com.bbhgroup.zeroone_task

enum class Role {
    USER, ADMIN, OPERATOR
}

enum class Languages(val key: String) {
    UZ("uz"),
    RU("ru"),
    ENG("en")
}

enum class InquiriesStatus {
    PENDING,
    COMPLETED
}


enum class ErrorCodes(val code: Int) {

    USER_NOT_FOUND(100),
    USER_ALREADY_EXISTS(101),


    INVALID_MESSAGE_TEXT(1),
    INVALID_FILE_ID(2),
    SESSION_NOT_FOUND_M(3),
    INVALID_SESSION_STATUS_M(4),
    MESSAGE_NOT_FOUND(5),
    PROHIBITED_UPDATE_MESSAGE(6),
    INVALID_SESSION_CLIENT_ID(7),


    EMPTY_QUEUE_CLIENT_ID(30),
    EMPTY_LIST_M(31),
    QUE_NOT_FOUND(32),
    QUE_OWNER_EXCEPTION(33),
    INVALID_MESSAGE_TYPE(34),

}

enum class BotSteps {

    START,
    SELECT_LANGUAGE,
    INPUT_FULLNAME,
    INPUT_PHONE_NUMBER,

}

enum class MessageType {
    TEXT, VOICE, AUDIO, VIDEO, GAME, STICKER, GIF, PHOTO
}