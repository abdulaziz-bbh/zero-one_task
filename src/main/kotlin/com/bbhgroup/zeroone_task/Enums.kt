package com.bbhgroup.zeroone_task

enum class UserRole{
    USER,ADMIN,OPERATOR
}

enum class Languages(val key:String){
    UZ("uz"),
    RU("ru"),
    ENG("en")
}

enum class InquiriesStatus{
    PENDING,
    COMPLETED
}
enum class BotSteps{

    START,
    SELECT_LANGUAGE,
    INPUT_FULLNAME,
    INPUT_PHONE_NUMBER,
    
}

enum class MessageType{
    TEXT, VOICE, AUDIO, VIDEO, GAME, STICKER, GIF, PHOTO
}