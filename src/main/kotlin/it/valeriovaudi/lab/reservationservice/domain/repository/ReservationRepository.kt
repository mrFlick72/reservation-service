package it.valeriovaudi.lab.reservationservice.domain.repository

import it.valeriovaudi.lab.reservationservice.domain.model.Reservation
import org.reactivestreams.Publisher

interface ReservationRepository {

    fun findOne(reservationId: String): Publisher<Reservation>

    fun save(reservation: Reservation): Publisher<Reservation>

    fun delete(reservationId: String): Publisher<Void>

}