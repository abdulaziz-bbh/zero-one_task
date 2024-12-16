package com.bbhgroup.zeroone_task

import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class ExceptionHandler(private val errorMessageSource: ResourceBundleMessageSource) {

    @ExceptionHandler(BillingExceptionHandler::class)
    fun handleAccountException(exception: BillingExceptionHandler): ResponseEntity<BaseMessage> {
        return ResponseEntity.badRequest().body(exception.getErrorMessage(errorMessageSource))
    }
}

@RestController
@RequestMapping("api/v1/message")
class MessageController(private val messageService: MessageService) {
    @GetMapping("/findBySession/{sessionId}")
    fun findAllBySessionId(@PathVariable sessionId: Long) = messageService.findAllBySessionId(sessionId)

    @GetMapping("/findByClient/{clientId}")
    fun findAllByClientId(@PathVariable clientId: Long) = messageService.findAllByClientId(clientId)

    @GetMapping("/findByOperator/{operatorId}")
    fun findAllByOperatorId(@PathVariable operatorId: Long) = messageService.findAllByOperatorId(operatorId)

    @PostMapping("/findByBetweenDates")
    fun findAllBetweenDates(@RequestBody request: MessageDateBetweenDto) = messageService.findAllBetweenDates(request)
}

@RestController
@RequestMapping("api/v1/user")
class UserController(val userService: UserService) {

    @GetMapping("{id}")
    fun getOne(@PathVariable id: Long) = userService.getOne(id)

    @DeleteMapping("{id}")
    fun delete(@PathVariable id: Long) = userService.deleteOne(id)

    @GetMapping("get-all")
    fun getAll(
        @RequestParam(required = false) role: String?,
        @RequestParam(required = false) startTime: String?,
        @RequestParam(required = false) endTime: String?,
        pageable: Pageable
    ) = userService.getAll(role, startTime, endTime, pageable)

    @PutMapping
    fun changeRole(@RequestParam id: Long, @RequestParam role: String) = userService.changeRole(id, role)
}


@RestController
@RequestMapping("api/v1/session")
class SessionController(val sessionService: SessionService) {

    @GetMapping("{id}")
    fun getOne(@PathVariable id: Long) = sessionService.getOne(id)

    @DeleteMapping("{id}")
    fun deleteOne(@PathVariable id: Long) = sessionService.deleteOne(id)

    @GetMapping
    fun getAll(
        @RequestParam(required = false) startTime: String?,
        @RequestParam(required = false) endTime: String?,
        pageable: Pageable
    ) = sessionService.getAll(startTime, endTime, pageable)

}