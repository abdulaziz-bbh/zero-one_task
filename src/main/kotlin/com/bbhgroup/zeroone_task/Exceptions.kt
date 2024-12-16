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

class DataHasAlreadyExistsException : BillingExceptionHandler() {
    override fun errorCode() = ErrorCodes.DATA_HAS_ALREADY_EXISTS
}

class UserHasAlreadyExistsException : BillingExceptionHandler() {
    override fun errorCode(): ErrorCodes {
        return ErrorCodes.USER_ALREADY_EXISTS
    }
}

class UserNotFoundException : BillingExceptionHandler() {
    override fun errorCode(): ErrorCodes {
        return ErrorCodes.USER_NOT_FOUND
    }
}

class UserBadRequestException : BillingExceptionHandler() {
    override fun errorCode() = ErrorCodes.USER_BAD_REQUEST
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

class EmptyListMException : BillingExceptionHandler() {
    override fun errorCode() = ErrorCodes.EMPTY_LIST_M
}

class InvalidMessageTypeException : BillingExceptionHandler() {
    override fun errorCode() = ErrorCodes.INVALID_MESSAGE_TYPE
}

class OperatorNotFoundException : BillingExceptionHandler() {
    override fun errorCode(): ErrorCodes {
        return ErrorCodes.OPERATOR_NOT_FOUND
    }
}

class SessionNotFoundException : BillingExceptionHandler() {
    override fun errorCode(): ErrorCodes {
        return ErrorCodes.SESSION_NOT_FOUND
    }
}











