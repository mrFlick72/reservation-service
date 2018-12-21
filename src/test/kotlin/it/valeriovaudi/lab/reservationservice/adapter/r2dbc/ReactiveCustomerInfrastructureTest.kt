package it.valeriovaudi.lab.reservationservice.adapter.r2dbc

import io.r2dbc.client.R2dbc
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration
import io.r2dbc.postgresql.PostgresqlConnectionFactory
import it.valeriovaudi.lab.reservationservice.domain.model.Customer
import org.hamcrest.core.Is
import org.junit.*
import org.junit.Assert.assertNull
import org.springframework.data.r2dbc.function.TransactionalDatabaseClient
import org.testcontainers.containers.DockerComposeContainer
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import java.io.File
import java.time.Duration
import java.util.*

class ReactiveCustomerInfrastructureTest   {

    companion object {
        @ClassRule
        @JvmField
        val container: DockerComposeContainer<*> = DockerComposeContainer<Nothing>(File("src/test/resources/docker-compose.yml"))
                .withExposedService("postgres_1", 5432)

    }

    @Before
    fun setUp() {
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

    @Test
    fun `save a customer not allowed tx rolbaked`() {
        val firstReservationId = UUID.randomUUID().toString()
        val secondReservationId = UUID.randomUUID().toString()
        val thirdReservationId = UUID.randomUUID().toString()

        val firstCustomer = newCustomer(prefix = "rolback", suffix = "1")
        val secondCustomer = newCustomer(prefix = "rolback", suffix = "2")
        val thirdCustomer = newCustomer(prefix = "rolback", suffix = "3")
        try {
            databaseClient.inTransaction {
                reactiveCustomerRepository.save(firstReservationId, firstCustomer)
                        .then(reactiveCustomerRepository.save(secondReservationId, secondCustomer))
                        .then(reactiveCustomerRepository.save(thirdReservationId, thirdCustomer))

                        .then(Mono.error<RuntimeException>({ RuntimeException() }))

                        .then()
            }.toMono().block(Duration.ofMinutes(1))
        } catch (e: Exception) {
        }

        Assert.assertTrue(findOneBy(firstReservationId)!!.size == 0)
        Assert.assertTrue(findOneBy(secondReservationId)!!.size == 0)
        Assert.assertTrue(findOneBy(thirdReservationId)!!.size == 0)
    }

    @Test
    fun `save a customer`() {

        val firstReservationId = UUID.randomUUID().toString()
        val secondReservationId = UUID.randomUUID().toString()
        val thirdReservationId = UUID.randomUUID().toString()


        val firstCustomer = newCustomer(prefix = "save", suffix = "1")
        val secondCustomer = newCustomer(prefix = "save", suffix = "2")
        val thirdCustomer = newCustomer(prefix = "save", suffix = "3")


        databaseClient.inTransaction {
            reactiveCustomerRepository.save(firstReservationId, firstCustomer)
                    .then(reactiveCustomerRepository.save(secondReservationId, secondCustomer))
                    .then(reactiveCustomerRepository.save(thirdReservationId, thirdCustomer))
                    .then()
        }.toMono().block(Duration.ofMinutes(1))

        Assert.assertTrue(findOneBy(firstReservationId)!!.size == 1)
        Assert.assertTrue(findOneBy(secondReservationId)!!.size == 1)
        Assert.assertTrue(findOneBy(thirdReservationId)!!.size == 1)
    }

    @Test
    fun `retrieve a customer`() {
        val firstReservationId = UUID.randomUUID().toString()
        val secondReservationId = UUID.randomUUID().toString()
        val thirdReservationId = UUID.randomUUID().toString()

        val firstCustomer = newCustomer(prefix = "New", suffix = "1")
        val secondCustomer = newCustomer(prefix = "New", suffix = "2")
        val thirdCustomer = newCustomer(prefix = "New", suffix = "3")


        databaseClient.inTransaction {
            reactiveCustomerRepository.save(firstReservationId, firstCustomer)
                    .then(reactiveCustomerRepository.save(secondReservationId, secondCustomer))
                    .then(reactiveCustomerRepository.save(thirdReservationId, thirdCustomer))
                    .then()
        }.toMono().block(Duration.ofMinutes(1))

        val customer = reactiveCustomerRepository.find(firstReservationId).block(Duration.ofMinutes(1))
        println(customer)
        Assert.assertNotNull(customer)
        Assert.assertThat(customer, Is.`is`(firstCustomer))
    }

    @Test
    fun `retrieve a no existing customer`() {
        val reservationId = UUID.randomUUID().toString()
        val customer = reactiveCustomerRepository.find(reservationId).block()
        println(customer)
        assertNull(customer)
    }

    @Test
    fun `delete a customer`() {
        val reservationId = UUID.randomUUID().toString()
        val firstCustomer = newCustomer(prefix = "save", suffix = "1")

        reactiveCustomerRepository.save(reservationId, firstCustomer).toMono().block(Duration.ofMinutes(1))
        reactiveCustomerRepository.delete(reservationId).toMono().block(Duration.ofMinutes(1))

        val customer = reactiveCustomerRepository.find(reservationId).block()
        println(customer)
        assertNull(customer)
    }

    fun newCustomer(prefix: String = "", suffix: String = "") = Customer("$prefix A_FIRST_NAME $suffix", "A_LAST_NAME")

    fun findOneBy(reservationId: String): MutableList<Customer>? = r2dbc.inTransaction { handle ->
        handle.select("SELECT * FROM customer WHERE reservation_id=$1", reservationId)
                .mapResult { sqlRowMap ->
                    sqlRowMap.map { t, u ->
                        Customer(t.get("first_name", String::class.java)!!,
                                t.get("last_name", String::class.java)!!)
                    }
                }
    }.collectList().block(Duration.ofMinutes(1))
}