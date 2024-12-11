package com.bbhgroup.zeroone_task


enum class ErrorCodes(val code:Int) {

    USER_NOT_FOUND(100),
    USER_ALREADY_EXISTS(101),
    USER_BAD_REQUEST(102)

}

enum class BotSteps{
    START,
    SELECT_LANGUAGE,
    SHARE_CONTACT,
    CONNECT_OPERATOR,
    SENDING_MESSAGES,
    END_CHAT
}
enum class MessageKeys{
    SHARE_CONTACT,
    SUCCESS_SHARE_CONTACT,
    ALREADY_REGISTERED,
    CANCEL_CHAT
}

enum class Role{
    USER,ADMIN,OPERATOR
}

enum class Languages(val key:String){
    UZ("uz"),
    RU("ru"),
    EN("en")
}

enum class MessageType{
    TEXT, VOICE, AUDIO, VIDEO, GAME, STICKER, GIF, PHOTO, VIDEO_NOTE, UNKNOWN
}