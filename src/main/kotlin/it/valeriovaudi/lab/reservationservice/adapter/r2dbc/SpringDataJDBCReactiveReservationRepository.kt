package it.valeriovaudi.lab.reservationservice.adapter.r2dbc

import it.valeriovaudi.lab.reservationservice.domain.model.Reservation
import it.valeriovaudi.lab.reservationservice.domain.repository.ReservationRepository
import org.reactivestreams.Publisher
import org.springframework.data.r2dbc.function.DatabaseClient

class SpringDataJDBCReactiveReservationRepository(private val databaseClient: DatabaseClient) : ReservationRepository {
    override fun findOne(reservationId: String): Publisher<Reservation> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun save(reservation: Reservation): Publisher<Reservation> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun delete(reservation: Reservation): Publisher<Reservation> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}
