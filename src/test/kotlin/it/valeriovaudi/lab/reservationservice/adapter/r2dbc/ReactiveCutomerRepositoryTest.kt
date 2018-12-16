package it.valeriovaudi.lab.reservationservice.adapter.r2dbc

import io.r2dbc.client.R2dbc
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration
import io.r2dbc.postgresql.PostgresqlConnectionFactory
import it.valeriovaudi.lab.reservationservice.domain.model.Customer
import junit.framework.Assert.assertNotNull
import org.hamcrest.core.Is
import org.junit.Assert
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.springframework.data.r2dbc.function.TransactionalDatabaseClient
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import java.time.Duration
import java.util.*

class ReactiveCutomerRepositoryTest {

    lateinit var postgresqlConnectionFactory: PostgresqlConnectionFactory
    lateinit var databaseClient: TransactionalDatabaseClient
    lateinit var reactiveCutomerRepository: ReactiveCutomerRepository
    lateinit var r2dbc: R2dbc

    @Before
    fun setUp() {
        postgresqlConnectionFactory = PostgresqlConnectionFactory(PostgresqlConnectionConfiguration.builder()
                .host("localhost")
                .database("reservation")
                .username("root")
                .password("root")
                .build())

        r2dbc = R2dbc(postgresqlConnectionFactory)
        databaseClient = TransactionalDatabaseClient.create(postgresqlConnectionFactory)
        reactiveCutomerRepository = ReactiveCutomerRepository(databaseClient)
    }

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
                reactiveCutomerRepository.save(firstReservationId, firstCustomer)
                        .then(reactiveCutomerRepository.save(secondReservationId, secondCustomer))
                        .then(reactiveCutomerRepository.save(thirdReservationId, thirdCustomer))

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
            reactiveCutomerRepository.save(firstReservationId, firstCustomer)
                    .then(reactiveCutomerRepository.save(secondReservationId, secondCustomer))
                    .then(reactiveCutomerRepository.save(thirdReservationId, thirdCustomer))
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
            reactiveCutomerRepository.save(firstReservationId, firstCustomer)
                    .then(reactiveCutomerRepository.save(secondReservationId, secondCustomer))
                    .then(reactiveCutomerRepository.save(thirdReservationId, thirdCustomer))
                    .then()
        }.toMono().block(Duration.ofMinutes(1))

        val customer = reactiveCutomerRepository.find(firstReservationId).block(Duration.ofMinutes(1))
        println(customer)
        assertNotNull(customer)
        Assert.assertThat(customer, Is.`is`(firstCustomer))
    }

    @Test
    fun `retrieve a no existing customer`() {
        val reservationId = UUID.randomUUID().toString()
        val customer = reactiveCutomerRepository.find(reservationId).block()
        println(customer)
        assertNull(customer)
    }

    @Test
    fun `delete a customer`() {
        val reservationId = UUID.randomUUID().toString()
        val firstCustomer = newCustomer(prefix = "save", suffix = "1")

        reactiveCutomerRepository.save(reservationId, firstCustomer).toMono().block(Duration.ofMinutes(1))
        reactiveCutomerRepository.delete(reservationId).toMono().block(Duration.ofMinutes(1))

        val customer = reactiveCutomerRepository.find(reservationId).block()
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