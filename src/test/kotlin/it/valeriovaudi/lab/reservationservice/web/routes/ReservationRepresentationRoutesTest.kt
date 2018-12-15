package it.valeriovaudi.lab.reservationservice.web.routes

import com.fasterxml.jackson.databind.ObjectMapper
import io.r2dbc.postgresql.PostgresqlConnectionFactory
import it.valeriovaudi.lab.reservationservice.adapter.r2dbc.ReactiveCutomerRepository
import it.valeriovaudi.lab.reservationservice.adapter.r2dbc.ReactiveReservationRepository
import it.valeriovaudi.lab.reservationservice.extractId
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.returnResult
import it.valeriovaudi.lab.reservationservice.web.representation.ReservationRepresentation
import it.valeriovaudi.lab.reservationservice.web.representation.CustomerRepresentation
import org.springframework.web.reactive.function.BodyInserters
import java.time.LocalDateTime

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner::class)
class ReservationRepresentationRoutesTest {

    @Autowired
    private lateinit var webClient: WebTestClient

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var postgresqlConnectionFactory: PostgresqlConnectionFactory

    @Autowired
    private lateinit var reactiveReservationRepository: ReactiveReservationRepository

    @Autowired
    private lateinit var reactiveCutomerRepository: ReactiveCutomerRepository


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
}