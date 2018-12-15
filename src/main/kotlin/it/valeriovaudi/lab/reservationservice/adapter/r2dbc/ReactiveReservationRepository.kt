package it.valeriovaudi.lab.reservationservice.adapter.r2dbc

import it.valeriovaudi.lab.reservationservice.domain.model.Reservation
import it.valeriovaudi.lab.reservationservice.domain.repository.CustomerRepository
import it.valeriovaudi.lab.reservationservice.domain.repository.ReservationRepository
import org.reactivestreams.Publisher
import org.springframework.data.r2dbc.function.TransactionalDatabaseClient
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import sun.management.VMOptionCompositeData
import java.time.LocalDateTime

class ReactiveReservationRepository(private val databaseClient: TransactionalDatabaseClient,
                                    private val customerRepository: CustomerRepository) : ReservationRepository {

    override fun findOne(reservationId: String): Publisher<Reservation> =
            databaseClient.inTransaction {
                customerRepository.find(reservationId).toMono()
                        .flatMap { customer ->
                            it.execute().sql("SELECT * FROM reservation WHERE reservation_id=$1")
                                    .bind("$1", reservationId)
                                    .exchange()
                                    .flatMap { sqlRowMap ->
                                        sqlRowMap.extract { t, u ->
                                            Reservation(t.get("reservation_id", String::class.java)!!,
                                                    t.get("restaurant_name", String::class.java)!!,
                                                    customer, t.get("date", LocalDateTime::class.java)!!)
                                        }.one()
                                    }
                        }
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


    override fun delete(reservationId: String): Publisher<Void> =
            databaseClient.inTransaction {
                customerRepository.delete(reservationId).toMono()
                        .then(it.execute().sql("DELETE FROM reservation WHERE reservation_id = $1")
                                .bind("$1", reservationId)
                                .fetch().rowsUpdated())
            }.then(Mono.empty())


}
