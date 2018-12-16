package it.valeriovaudi.lab.reservationservice.web.routes

import it.valeriovaudi.lab.reservationservice.adapter.r2dbc.ReactiveReservationRepository
import it.valeriovaudi.lab.reservationservice.domain.model.Customer
import it.valeriovaudi.lab.reservationservice.domain.model.Reservation
import it.valeriovaudi.lab.reservationservice.extractId
import it.valeriovaudi.lab.reservationservice.web.representation.CustomerRepresentation
import it.valeriovaudi.lab.reservationservice.web.representation.ReservationRepresentation
import org.hamcrest.core.Is
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.returnResult
import org.springframework.web.reactive.function.BodyInserters
import reactor.core.publisher.toMono
import java.time.Duration
import java.time.LocalDateTime
import java.util.*

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner::class)
class ReservationRepresentationRoutesTest {

    @Autowired
    private lateinit var webClient: WebTestClient

    @Autowired
    private lateinit var reactiveReservationRepository: ReactiveReservationRepository

    @Test
    fun `book a new reservation`() {
        val location = this.webClient.post()
                .uri("/reservation")
                .body(BodyInserters.fromObject(ReservationRepresentation(restaurantName = "A_RESTAURANT_NAME",
                        customer = CustomerRepresentation("FIRST_NAME", "LAST_NAME"),
                        date = LocalDateTime.of(2018, 1, 1, 10, 10))))
                .exchange()
                .expectStatus().isCreated
                .returnResult<Any>().responseHeaders.location

        println(location!!.extractId())
        Assert.assertNotNull(location.extractId())
    }

    @Test
    fun `find a new reservation`() {
        val reservationId = UUID.randomUUID().toString()
        reactiveReservationRepository.save(Reservation(reservationId, "A_RESTAURANT_NAME",
                Customer(reservationId, "FIRST_NAME", "LAST_NAME"),
                LocalDateTime.of(2018, 1, 1, 10, 10)))
                .toMono().block(Duration.ofMinutes(1))
        val expected = ReservationRepresentation(restaurantName = "A_RESTAURANT_NAME",
                customer = CustomerRepresentation("FIRST_NAME", "LAST_NAME"),
                date = LocalDateTime.of(2018, 1, 1, 10, 10))

        val actual = this.webClient.get()
                .uri("/reservation/$reservationId")
                .exchange()
                .expectStatus().isOk
                .expectBody(ReservationRepresentation::class.java)
                .returnResult().responseBody

        Assert.assertThat(expected, Is.`is`(actual))
    }
}