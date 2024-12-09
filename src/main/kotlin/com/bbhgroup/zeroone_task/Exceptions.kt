package com.bbhgroup.zeroone_task

import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.context.support.ResourceBundleMessageSource

sealed class BillingExceptionHandler() : RuntimeException() {
    abstract fun errorCode(): ErrorCodes
    open fun getArguments(): Array<Any?>? = null

    fun getErrorMessage(resourceBundleMessageSource: ResourceBundleMessageSource): BaseMessage {
        val message = try {
            resourceBundleMessageSource.getMessage(
                errorCode().name, getArguments(), LocaleContextHolder.getLocale()
            )
        } catch (e: Exception) {
            e.message ?: "Unknown error"
        }
        return BaseMessage(errorCode().code, message)
    }
}

class UserAlreadyExistsException : BillingExceptionHandler() {
    override fun errorCode(): ErrorCodes {
        return ErrorCodes.USER_ALREADY_EXISTS
    }
}

class UserNotFoundException : BillingExceptionHandler() {
    override fun errorCode(): ErrorCodes {
        return ErrorCodes.USER_NOT_FOUND
    }
}

class InvalidFileIdException : BillingExceptionHandler() {
    override fun errorCode(): ErrorCodes {
        return ErrorCodes.INVALID_FILE_ID
    }
}

class InvalidMessageTextException : BillingExceptionHandler() {
    override fun errorCode(): ErrorCodes {
        return ErrorCodes.INVALID_MESSAGE_TEXT
    }
}

class SessionNotFoundMException : BillingExceptionHandler() {
    override fun errorCode(): ErrorCodes {
        return ErrorCodes.SESSION_NOT_FOUND_M
    }
}

class InvalidSessionStatusMException : BillingExceptionHandler() {
    override fun errorCode(): ErrorCodes {
        return ErrorCodes.INVALID_SESSION_STATUS_M
    }
}

class MessageNotFoundException : BillingExceptionHandler() {
    override fun errorCode(): ErrorCodes {
        return ErrorCodes.MESSAGE_NOT_FOUND
    }
}

class ProhibitedUpdateMessageException : BillingExceptionHandler() {
    override fun errorCode() = ErrorCodes.PROHIBITED_UPDATE_MESSAGE
}

class InvalidSessionClientIdException : BillingExceptionHandler() {
    override fun errorCode() = ErrorCodes.INVALID_SESSION_CLIENT_ID
}

class EmptyQueueClientIdException : BillingExceptionHandler() {
    override fun errorCode() = ErrorCodes.EMPTY_QUEUE_CLIENT_ID
}

class QueueNotFoundException : BillingExceptionHandler() {
    override fun errorCode() = ErrorCodes.QUE_NOT_FOUND
}

class EmptyListMException : BillingExceptionHandler() {
    override fun errorCode() = ErrorCodes.EMPTY_LIST_M
}

class QueueOwnerException : BillingExceptionHandler() {
    override fun errorCode() = ErrorCodes.QUE_OWNER_EXCEPTION
}

class InvalidMessageTypeException : BillingExceptionHandler() {
    override fun errorCode() = ErrorCodes.INVALID_MESSAGE_TYPE
}










