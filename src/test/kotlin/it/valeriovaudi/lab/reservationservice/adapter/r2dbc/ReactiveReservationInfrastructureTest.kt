package it.valeriovaudi.lab.reservationservice.adapter.r2dbc

import io.r2dbc.client.R2dbc
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration
import io.r2dbc.postgresql.PostgresqlConnectionFactory
import it.valeriovaudi.lab.reservationservice.domain.model.Customer
import it.valeriovaudi.lab.reservationservice.domain.model.Reservation
import org.hamcrest.core.Is
import org.junit.*
import org.springframework.data.r2dbc.function.TransactionalDatabaseClient
import org.testcontainers.containers.DockerComposeContainer
import reactor.core.publisher.toMono
import java.io.File
import java.time.Duration
import java.time.LocalDateTime
import java.util.*

class ReactiveReservationInfrastructureTest  {

    companion object {
        @ClassRule
        @JvmField
        val container: DockerComposeContainer<*> = DockerComposeContainer<Nothing>(File("src/test/resources/docker-compose.yml"))
                .withExposedService("postgres_1", 5432)

    }
    @Before
    fun setUp() {
        /**
         * I prefer do not use docker port redirect in order to prevents the port conflicts on container start,
         * imaging it on a concurrent test suite, the code below is necessary in order to get the host and port
         * that the docker runtime assign to the container
         * */
        val serviceHost = container.getServiceHost("postgres_1", 5432)
        val servicePort = container.getServicePort("postgres_1", 5432)

        postgresqlConnectionFactory = PostgresqlConnectionFactory(PostgresqlConnectionConfiguration.builder()
                .host(serviceHost)
                .port(servicePort)
                .database("reservation")
                .username("root")
                .password("root")
                .build())

        databaseClient = TransactionalDatabaseClient.create(postgresqlConnectionFactory)
        reactiveCustomerRepository = ReactiveCustomerRepository(databaseClient)
        reactiveReservationRepository = ReactiveReservationRepository(databaseClient, reactiveCustomerRepository)

        r2dbc = R2dbc(postgresqlConnectionFactory)
    }

    lateinit var postgresqlConnectionFactory: PostgresqlConnectionFactory
    lateinit var databaseClient: TransactionalDatabaseClient
    lateinit var reactiveReservationRepository: ReactiveReservationRepository
    lateinit var reactiveCustomerRepository: ReactiveCustomerRepository
    lateinit var r2dbc: R2dbc

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