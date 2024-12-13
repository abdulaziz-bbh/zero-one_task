package com.bbhgroup.zeroone_task

import org.springframework.stereotype.Component

@Component
class StatisticsMapper {

    fun toTotalSessionsResponse(totalSessions: Long, totalActiveSessions: Long): TotalSessionsResponse {
        return TotalSessionsResponse(
            totalSessions = totalSessions,
            totalActiveSessions = totalActiveSessions
        )
    }

    fun toOperatorSessionStatisticsResponse(
        operatorId: Long,
        operatorName: String,
        sessions: List<Session>
    ): OperatorSessionStatisticsResponse {
        val totalHandledSessions = sessions.size.toLong()
        val activeSessions = sessions.count { it.isActive }.toLong()
        val ratings = sessions.mapNotNull { it.rate }
        val averageRating = if (ratings.isNotEmpty()) ratings.average() else 0.0

        return OperatorSessionStatisticsResponse(
            operatorId = operatorId,
            operatorName = operatorName,
            totalHandledSessions = totalHandledSessions,
            averageRating = averageRating,
            activeSessions = activeSessions
        )
    }

    fun toDetailedRatingResponse(sessions: List<Session>): DetailedRatingResponse {
        val ratings = sessions.mapNotNull { it.rate }
        val detailedRatings = ratings.groupingBy { it }.eachCount()
        return DetailedRatingResponse(
            totalRatings = ratings.size.toLong(),
            averageRating = if (ratings.isNotEmpty()) ratings.average() else 0.0,
            detailedRatings = detailedRatings.mapValues { it.value.toLong() }
        )
    }

    fun toUserStatisticsResponse(user: UserEntity, sessions: List<Session>): UserStatisticsResponse {
        val totalSessions = sessions.size.toLong()
        val activeSessions = sessions.count { it.isActive }.toLong()
        val ratings = sessions.mapNotNull { it.rate }
        val totalRatings = ratings.size.toLong()
        val averageRating = if (ratings.isNotEmpty()) ratings.average() else 0.0

        return UserStatisticsResponse(
            userId = user.id!!,
            fullName = user.fullName,
            totalSessions = totalSessions,
            activeSessions = activeSessions,
            totalRatings = totalRatings,
            averageRating = averageRating
        )
    }

    fun toTopRatedOperatorResponse(
        operator: UserEntity,
        averageRating: Double,
        totalRatings: Long
    ): TopRatedOperatorResponse {
        return TopRatedOperatorResponse(
            operatorId = operator.id!!,
            operatorName = operator.fullName,
            averageRating = averageRating,
            totalRatings = totalRatings
        )
    }
}

