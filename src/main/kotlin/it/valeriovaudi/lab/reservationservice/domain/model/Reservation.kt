package it.valeriovaudi.lab.reservationservice.domain.model

import java.time.LocalDateTime

data class Reservation(val reservationId: String, val restaurantName: String, val customer: Customer, val date: LocalDateTime)