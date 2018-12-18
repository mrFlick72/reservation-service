package it.valeriovaudi.lab.reservationservice.adapter.r2dbc

import it.valeriovaudi.lab.reservationservice.domain.model.Customer
import it.valeriovaudi.lab.reservationservice.domain.model.Reservation
import org.hamcrest.core.Is
import org.junit.Assert
import org.junit.Test
import reactor.core.publisher.toMono
import java.time.Duration
import java.time.LocalDateTime
import java.util.*

class ReactiveReservationRepositoryTest : AbstractRepositoryTestRunner() {

    private val A_DATE = LocalDateTime.of(2018, 1, 1, 22, 0)
    private val A_RESTAURANT_NAME = "A_RESTAURANT_NAME"
    private val A_FIRST_NAME = "A_FIRST_NAME"
    private val A_LAST_NAME = "A_LAST_NAME"

    @Test
    fun `make a new reservation`() {
        val reservationId = UUID.randomUUID().toString()
        val reservationDate = A_DATE
        val customer = Customer(A_FIRST_NAME, A_LAST_NAME)
        val restaurantName = A_RESTAURANT_NAME

        val expected = Reservation(reservationId, restaurantName, customer, reservationDate)
        reactiveReservationRepository.save(Reservation(reservationId, restaurantName, customer, reservationDate))
                .toMono().block(Duration.ofMinutes(1))


        Assert.assertThat(findOneBy(reservationId), Is.`is`(expected))
    }


    @Test
    fun `find a new reservation by reservation id`() {
        val reservationId = UUID.randomUUID().toString()
        val reservationDate = A_DATE
        val customer = Customer(A_FIRST_NAME, A_LAST_NAME)
        val restaurantName = A_RESTAURANT_NAME

        val expected = Reservation(reservationId, restaurantName, customer, reservationDate)
        reactiveReservationRepository.save(Reservation(reservationId, restaurantName, customer, reservationDate))
                .toMono().block(Duration.ofMinutes(1))

        val actual = reactiveReservationRepository.findOne(reservationId)
                .toMono().block(Duration.ofMinutes(1))

        println(actual)
        Assert.assertThat(actual, Is.`is`(expected))

    }

    @Test
    fun `delete a reservation`() {
        val reservationId = UUID.randomUUID().toString()
        val reservationDate = A_DATE
        val customer = Customer(A_FIRST_NAME, A_LAST_NAME)
        val restaurantName = A_RESTAURANT_NAME

        reactiveReservationRepository.save(Reservation(reservationId, restaurantName, customer, reservationDate))
                .toMono().block(Duration.ofMinutes(1))


        reactiveReservationRepository.delete(reservationId)
                .toMono().block(Duration.ofMinutes(1))


        val actual = reactiveReservationRepository.findOne(reservationId)
                .toMono().block(Duration.ofMinutes(1))

        println(actual)
        Assert.assertNull(actual)
    }

    fun findOneBy(reservationId: String) = r2dbc.inTransaction { handle ->
        handle.select("SELECT * FROM customer WHERE reservation_id=$1", reservationId)
                .mapResult { sqlRowMap ->
                    sqlRowMap.map { t, u ->
                        Customer(t.get("first_name", String::class.java)!!,
                                t.get("last_name", String::class.java)!!)
                    }
                }
                .flatMap { customer ->
                    handle.select("SELECT * FROM reservation WHERE reservation_id=$1", reservationId)
                            .mapResult { sqlRowMap ->
                                sqlRowMap.map { t, u ->
                                    Reservation(reservationId,
                                            t.get("restaurant_name", String::class.java)!!,
                                            customer, t.get("date", LocalDateTime::class.java)!!)
                                }
                            }
                }
    }.toMono().block(Duration.ofMinutes(1))

}