package com.bbhgroup.zeroone_task


data class BaseMessage(val code: Int, val message: String?)

data class UserCreateRequest(
        val fullName: String,
        val phoneNumber: String,
        val chatId: Long,
        val language: Set<Languages>
){
    fun toEntity(role: Role):UserEntity{
        return UserEntity(fullName, phoneNumber, chatId, role, language)
    }
}

data class UserResponse(
    val id: Long,
    val fullName: String,
    val phoneNumber: String,
    val chatId: Long,
    val language: Set<Languages?>
){
    companion object{
        fun toResponse(userEntity: UserEntity):UserResponse{
            userEntity.run {
                return UserResponse(id!!,fullName, phoneNumber, chatId, language)
            }
        }
    }
}

data class UserUpdateRequest(
        var fullName: String?,
        var phoneNumber: String?,
        var language: Set<Languages>?
)


