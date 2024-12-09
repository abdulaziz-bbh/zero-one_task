package com.bbhgroup.zeroone_task

enum class Role{
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




enum class ErrorCodes(val code:Int) {

    USER_NOT_FOUND(100),
    USER_ALREADY_EXISTS(101)

}
enum class BotSteps{

    START,
    SELECT_LANGUAGE,
    SHARE_CONTACT

}

enum class MessageType{
    TEXT, VOICE, AUDIO, VIDEO, GAME, STICKER, GIF, PHOTO
}