package com.bbhgroup.zeroone_task
import org.springframework.context.support.ResourceBundleMessageSource
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




