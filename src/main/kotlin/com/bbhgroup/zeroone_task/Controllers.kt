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
@RequestMapping("/api/statistics")
class StatisticsController(private val service: StatisticsService) {

    @GetMapping("/total-sessions")
    fun getTotalSessions() = service.getTotalSessions()


    @GetMapping("/operator-sessions/{operatorId}")
    fun getOperatorSessionStatistics(@PathVariable operatorId: Long) =
        service.getOperatorSessionStatistics(operatorId)


    @GetMapping("/detailed-ratings")
    fun getDetailedRatings() = service.getDetailedRatings()


    @GetMapping("/user-statistics")
    fun getUserStatistics() = service.getUserStatistics()


    @GetMapping("/top-rated-operators")
    fun getTopRatedOperators(
        @RequestParam(value = "lastMonth", defaultValue = "true") lastMonth: Boolean,
        @RequestParam(value = "limit", defaultValue = "10") limit: Int) =
        service.getTopRatedOperators(lastMonth, limit)

    @GetMapping("/highest-rated-operator")
    fun getHighestRatedOperator() = service.getTopRatedOperator()

    @GetMapping("/lowest-rated-operator")
    fun getLowestRatedOperator() = service.getLowestRatedOperator()

    @GetMapping("/average-ratings-by-operator")
    fun getAverageRatingsByOperator() = service.getOperatorAverageRatings()

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

    @GetMapping("get-one-operator")
    fun getOneOperator(@RequestParam id: Long) = userService.findOperatorById(id)

    @DeleteMapping("{id}")
    fun delete(@PathVariable id: Long) = userService.deleteOne(id)

    @GetMapping("get-all")
    fun getAll(
            @RequestParam(required = false) role: String?,
            @RequestParam(required = false) startTime: String?,
            @RequestParam(required = false) endTime: String?,
            pageable: Pageable
    ) = userService.getAll(role, startTime, endTime, pageable)

    @GetMapping("get-all-by-status")
    fun getAllOperatorByStatus(@RequestParam status: String, pageable: Pageable) = userService.findAllOperatorByStatus(status, pageable)

    @GetMapping("get-all-by-rate")
    fun getAllOperatorWithRate(pageable: Pageable)=userService.findAllOperatorWithRate(pageable)
    @PutMapping
    fun changeRole(@RequestParam id: Long, @RequestParam role: String) = userService.changeRole(id, role)

    @PutMapping("{id}")
    fun addLangToOperator(@PathVariable id: Long, lang: String) = userService.addLanguageToOperator(id, lang)
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