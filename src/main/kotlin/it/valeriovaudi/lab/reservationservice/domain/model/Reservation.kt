package it.valeriovaudi.lab.reservationservice.domain.model

import java.util.*

data class Reservation(val restaurant : Restaurant, val customer : Customer, val date: Date)