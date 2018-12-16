package it.valeriovaudi.lab.reservationservice.web.representation

import it.valeriovaudi.lab.reservationservice.domain.model.Customer
import it.valeriovaudi.lab.reservationservice.domain.model.Reservation
import java.time.LocalDateTime
import java.util.*

data class ReservationRepresentation(var restaurantName: String, var customer: CustomerRepresentation, var date: LocalDateTime) {
    companion object {
        fun toDomain(reservationId: String = UUID.randomUUID().toString(),
                     reservationRepresentation: ReservationRepresentation): Reservation =
                Reservation(reservationId, reservationRepresentation.restaurantName,
                        Customer(reservationId, reservationRepresentation.customer.firstName, reservationRepresentation.customer.lastName),
                        reservationRepresentation.date)

        fun toRepresentation(reservation: Reservation): ReservationRepresentation =
                ReservationRepresentation(reservation.restaurantName,
                        CustomerRepresentation(reservation.customer.firstName, reservation.customer.lastName),
                        reservation.date)
    }
}