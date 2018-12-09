package it.valeriovaudi.lab.reservationservice.adapter.r2dbc

import io.r2dbc.postgresql.PostgresqlConnectionConfiguration
import io.r2dbc.postgresql.PostgresqlConnectionFactory
import it.valeriovaudi.lab.reservationservice.domain.model.Customer
import junit.framework.Assert.assertNotNull
import org.hamcrest.core.Is
import org.junit.Assert
import org.junit.Before

import org.junit.Test
import org.springframework.data.r2dbc.function.TransactionalDatabaseClient
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import java.lang.RuntimeException
import java.util.*

class SpringDataJDBCReactiveCutomerRepositoryTest {

    lateinit var postgresqlConnectionFactory: PostgresqlConnectionFactory;
    lateinit var databaseClient: TransactionalDatabaseClient;
    lateinit var reactiveCutomerRepository: SpringDataJDBCReactiveCutomerRepository;

    @Before
    fun setUp() {
        postgresqlConnectionFactory = PostgresqlConnectionFactory(PostgresqlConnectionConfiguration.builder()
                .host("localhost")
                .database("reservation")
                .username("root")
                .password("root")
                .build())


        databaseClient = TransactionalDatabaseClient.create(postgresqlConnectionFactory)
        reactiveCutomerRepository = SpringDataJDBCReactiveCutomerRepository(databaseClient)
    }

    @Test
    fun `save a customer not allowed tx rolbaked`() {

        databaseClient.inTransaction {
            reactiveCutomerRepository.save(Customer(UUID.randomUUID().toString(), "New Valerio1", "Vaudi"))
                    .then(reactiveCutomerRepository.save(Customer(UUID.randomUUID().toString(), "New Valerio2", "Vaudi")))
                    .then(reactiveCutomerRepository.save(Customer(UUID.randomUUID().toString(), "New Valerio3", "Vaudi")))

                    .then(Mono.error<RuntimeException>(RuntimeException()))

                    .then()
        }.toMono().block()
    }

    @Test
    fun `save a customer`() {

        databaseClient.inTransaction {
            reactiveCutomerRepository.save(Customer(UUID.randomUUID().toString(), "New Valerio1", "Vaudi"))
                    .then(reactiveCutomerRepository.save(Customer(UUID.randomUUID().toString(), "New Valerio2", "Vaudi")))
                    .then(reactiveCutomerRepository.save(Customer(UUID.randomUUID().toString(), "New Valerio3", "Vaudi")))
                    .then()
        }.toMono().block()
    }

    @Test
    fun `retrieve a customer`() {
        val firstReservation = UUID.randomUUID().toString()
        val secondReservation = UUID.randomUUID().toString()
        val thirdReservation = UUID.randomUUID().toString()


        databaseClient.inTransaction {
            reactiveCutomerRepository.save(Customer(firstReservation, "New Valerio1", "Vaudi"))
                    .then(reactiveCutomerRepository.save(Customer(secondReservation, "New Valerio2", "Vaudi")))
                    .then(reactiveCutomerRepository.save(Customer(thirdReservation, "New Valerio3", "Vaudi")))
                    .then()
        }.toMono().block()

        val customer = reactiveCutomerRepository.find(firstReservation).block()
        println(customer)
        assertNotNull(customer)
        Assert.assertThat(customer, Is.`is`(Customer(firstReservation, "New Valerio1", "Vaudi")))
    }

}