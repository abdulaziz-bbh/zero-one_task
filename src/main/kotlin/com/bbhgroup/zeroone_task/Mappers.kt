package com.bbhgroup.zeroone_task

import org.springframework.stereotype.Component

@Component
class RatingMapper {

    fun toDto(rating: RatingEntity): RatingResponse {
        return rating.run {
            RatingResponse(
                id = this.id,
                rate = this.rate,
                clientName = this.client.fullName,
                operatorName = this.operator.fullName,
                sessionId = this.session.id
            )
        }
    }

    fun toEntity(createRequest: RatingCreateRequest, client: UserEntity, operator: UserEntity, session: Session): RatingEntity {
        return createRequest.run {
            RatingEntity(
                rate = this.rate,
                client = client,
                operator = operator,
                session = session
            )
        }
    }

    fun updateEntity(rating: RatingEntity, updateRequest: RatingUpdateRequest): RatingEntity {
        return updateRequest.run {
            rating.apply {
                updateRequest.rate.let { this.rate = it }
            }
        }
    }
}
