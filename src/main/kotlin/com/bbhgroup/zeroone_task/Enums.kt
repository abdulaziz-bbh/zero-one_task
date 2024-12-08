package com.bbhgroup.zeroone_task

enum class Role{
    USER,ADMIN,OPERATOR
}

enum class Languages{
    UZ,RU,ENG
}

enum class InquiriesStatus{
    PENDING,
    COMPLETED
}




enum class ErrorCodes(val code:Int) {

    USER_NOT_FOUND(100),
    USER_ALREADY_EXISTS(101)

}