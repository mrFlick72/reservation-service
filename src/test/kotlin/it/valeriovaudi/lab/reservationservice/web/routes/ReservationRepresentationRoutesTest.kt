package it.valeriovaudi.lab.reservationservice.web.routes

import io.r2dbc.postgresql.PostgresqlConnectionConfiguration
import io.r2dbc.postgresql.PostgresqlConnectionFactory
import it.valeriovaudi.lab.reservationservice.adapter.r2dbc.ReactiveReservationRepository
import it.valeriovaudi.lab.reservationservice.domain.model.Customer
import it.valeriovaudi.lab.reservationservice.domain.model.Reservation
import it.valeriovaudi.lab.reservationservice.extractId
import it.valeriovaudi.lab.reservationservice.web.representation.CustomerRepresentation
import it.valeriovaudi.lab.reservationservice.web.representation.ReservationRepresentation
import junit.framework.Assert.fail
import org.hamcrest.core.Is
import org.junit.AfterClass
import org.junit.Assert
import org.junit.ClassRule
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.returnResult
import org.springframework.web.reactive.function.BodyInserters
import org.testcontainers.containers.DockerComposeContainer
import reactor.core.publisher.toMono
import java.io.File
import java.time.Duration
import java.time.LocalDateTime
import java.util.*

@DirtiesContext
@Import(RepoConfig::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner::class)
class ReservationRepresentationRoutesTest  {

    companion object {
        @ClassRule
        @JvmField
        val container: DockerComposeContainer<*> = DockerComposeContainer<Nothing>(File("src/test/resources/docker-compose.yml"))
                .withExposedService("postgres_1", 5432)

    }

    @Autowired
    private lateinit var webClient: WebTestClient

    @Autowired
    private lateinit var reactiveReservationRepository: ReactiveReservationRepository

    private val A_RESTAURANT_NAME = "A_RESTAURANT_NAME"
    private val FIRST_NAME = "FIRST_NAME"
    private val LAST_NAME = "LAST_NAME"
    private val A_DATE = LocalDateTime.of(2018, 1, 1, 10, 10)

    @Test
    fun `book a new reservation`() {
        val location = this.webClient.post()
                .uri("/reservation")
                .body(BodyInserters.fromObject(ReservationRepresentation(restaurantName = A_RESTAURANT_NAME,
                        customer = CustomerRepresentation(FIRST_NAME, LAST_NAME),
                        date = A_DATE)))
                .exchange()
                .expectStatus().isCreated
                .returnResult<Any>().responseHeaders.location

        println(location!!.extractId())
        Assert.assertNotNull(location.extractId())
    }

    @Test
    fun `find a new reservation`() {
        val reservationId = UUID.randomUUID().toString()
        reactiveReservationRepository.save(Reservation(reservationId, A_RESTAURANT_NAME,
                Customer(FIRST_NAME, LAST_NAME),
                A_DATE))
                .toMono().block(Duration.ofMinutes(1))
        val expected = ReservationRepresentation(restaurantName = A_RESTAURANT_NAME,
                customer = CustomerRepresentation(FIRST_NAME, LAST_NAME),
                date = A_DATE)

        val actual = this.webClient.get()
                .uri("/reservation/$reservationId")
                .exchange()
                .expectStatus().isOk
                .expectBody(ReservationRepresentation::class.java)
                .returnResult().responseBody

        Assert.assertThat(expected, Is.`is`(actual))
    }

    @Test
    fun `delete a reservation`() {
        val reservationId = UUID.randomUUID().toString()
        reactiveReservationRepository.save(Reservation(reservationId, A_RESTAURANT_NAME,
                Customer(FIRST_NAME, LAST_NAME),
                A_DATE))
                .toMono().block(Duration.ofMinutes(1))
        val expected = ReservationRepresentation(restaurantName = A_RESTAURANT_NAME,
                customer = CustomerRepresentation(FIRST_NAME, LAST_NAME),
                date = A_DATE)

        val actual = this.webClient.get()
                .uri("/reservation/$reservationId")
                .exchange()
                .expectStatus().isOk
                .expectBody(ReservationRepresentation::class.java)
                .returnResult().responseBody

        Assert.assertThat(expected, Is.`is`(actual))

        this.webClient.delete()
                .uri("/reservation/$reservationId")
                .exchange()
                .expectStatus().isNoContent

        reactiveReservationRepository.findOne(reservationId)
                .toMono().blockOptional(Duration.ofMinutes(1))
                .ifPresent { fail() }
    }
}

@Configuration
class RepoConfig {

    @Bean
    @Primary
    fun connectionFactory(): PostgresqlConnectionFactory  =
            PostgresqlConnectionFactory(PostgresqlConnectionConfiguration.builder()
                    .host(ReservationRepresentationRoutesTest.container.getServiceHost("postgres_1", 5432))
                    .port(ReservationRepresentationRoutesTest.container.getServicePort("postgres_1", 5432))
                    .database("reservation")
                    .username("root")
                    .password("root")
                    .build())
}