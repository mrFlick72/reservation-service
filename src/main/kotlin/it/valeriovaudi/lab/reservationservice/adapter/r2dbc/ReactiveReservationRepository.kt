package it.valeriovaudi.lab.reservationservice.adapter.r2dbc

import it.valeriovaudi.lab.reservationservice.domain.model.Reservation
import it.valeriovaudi.lab.reservationservice.domain.repository.CustomerRepository
import it.valeriovaudi.lab.reservationservice.domain.repository.ReservationRepository
import org.reactivestreams.Publisher
import org.springframework.data.r2dbc.function.TransactionalDatabaseClient
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono

class ReactiveReservationRepository(private val databaseClient: TransactionalDatabaseClient,
                                    private val customerRepository: CustomerRepository) : ReservationRepository {
    override fun findOne(reservationId: String): Publisher<Reservation> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun save(reservation: Reservation): Publisher<Reservation> =
            databaseClient.inTransaction {
                customerRepository.save(reservation.customer).toMono()
                        .then(it.execute().sql("INSERT INTO reservation (reservation_id, restaurant_name, date) VALUES ($1, $2, $3)")
                                .bind("$1", reservation.reservationId)
                                .bind("$2", reservation.restaurantName)
                                .bind("$3", reservation.date)
                                .fetch().rowsUpdated())
            }.then(Mono.just(reservation))


    override fun delete(reservation: Reservation): Publisher<Reservation> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}
