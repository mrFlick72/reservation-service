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
import java.util.*

class SpringDataJDBCReactiveCustomerRepositoryTest {

    lateinit var postgresqlConnectionFactory: PostgresqlConnectionFactory;
    lateinit var databaseClient: TransactionalDatabaseClient;
    lateinit var reactiveCutomerRepository: SpringDataJDBCReactiveCutomerRepository;
    val r2dbc = R2dbc(PostgresqlConnectionFactory(PostgresqlConnectionConfiguration.builder()
            .host("localhost")
            .database("reservation")
            .username("root")
            .password("root")
            .build()))

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
        val firstCustomer = newCustomer(UUID.randomUUID().toString(), prefix = "rolback", suffix = "1")
        val secondCustomer = newCustomer(UUID.randomUUID().toString(), prefix = "rolback", suffix = "2")
        val thirdCustomer = newCustomer(UUID.randomUUID().toString(), prefix = "rolback", suffix = "3")
        try {
            databaseClient.inTransaction {
                reactiveCutomerRepository.save(firstCustomer)
                        .then(reactiveCutomerRepository.save(secondCustomer))
                        .then(reactiveCutomerRepository.save(thirdCustomer))

                        .then(Mono.error<RuntimeException>({ RuntimeException() }))

                        .then()
            }.toMono().block()
        } catch (e: Exception) {
        }

        Assert.assertTrue(findOneBy(firstCustomer.reservationId)!!.size == 0)
        Assert.assertTrue(findOneBy(secondCustomer.reservationId)!!.size == 0)
        Assert.assertTrue(findOneBy(thirdCustomer.reservationId)!!.size == 0)
    }

    @Test
    fun `save a customer`() {
        val firstCustomer = newCustomer(UUID.randomUUID().toString(), prefix = "save", suffix = "1")
        val secondCustomer = newCustomer(UUID.randomUUID().toString(), prefix = "save", suffix = "2")
        val thirdCustomer = newCustomer(UUID.randomUUID().toString(), prefix = "save", suffix = "3")


        databaseClient.inTransaction {
            reactiveCutomerRepository.save(firstCustomer)
                    .then(reactiveCutomerRepository.save(secondCustomer))
                    .then(reactiveCutomerRepository.save(thirdCustomer))
                    .then()
        }.toMono().block()

        Assert.assertTrue(findOneBy(firstCustomer.reservationId)!!.size == 1)
        Assert.assertTrue(findOneBy(secondCustomer.reservationId)!!.size == 1)
        Assert.assertTrue(findOneBy(thirdCustomer.reservationId)!!.size == 1)
    }

    @Test
    fun `retrieve a customer`() {
        val firstCustomer = newCustomer(UUID.randomUUID().toString(), prefix = "New", suffix = "1")
        val secondCustomer = newCustomer(UUID.randomUUID().toString(), prefix = "New", suffix = "2")
        val thirdCustomer = newCustomer(UUID.randomUUID().toString(), prefix = "New", suffix = "3")


        databaseClient.inTransaction {
            reactiveCutomerRepository.save(firstCustomer)
                    .then(reactiveCutomerRepository.save(secondCustomer))
                    .then(reactiveCutomerRepository.save(thirdCustomer))
                    .then()
        }.toMono().block()

        val customer = reactiveCutomerRepository.find(firstCustomer.reservationId).block()
        println(customer)
        assertNotNull(customer)
        Assert.assertThat(customer, Is.`is`(firstCustomer))
    }

    @Test
    fun `retrieve a no existing customer`() {
        val firstCustomer = newCustomer(UUID.randomUUID().toString(), prefix = "New", suffix = "1")
        val customer = reactiveCutomerRepository.find(firstCustomer.reservationId).block()
        println(customer)
        assertNull(customer)
    }

    fun newCustomer(id: String, prefix: String = "", suffix: String = "") = Customer(id, "$prefix Valerio $suffix", "Vaudi")

    fun findOneBy(reservationId: String): MutableList<Customer>? = r2dbc.inTransaction { handle ->
        handle.select("SELECT * FROM customer WHERE reservation_id=$1", reservationId)
                .mapResult { sqlRowMap ->
                    sqlRowMap.map { t, u ->
                        Customer(t.get("reservation_id", String::class.java)!!,
                                t.get("first_name", String::class.java)!!,
                                t.get("last_name", String::class.java)!!)
                    }
                }
    }.collectList().block()
}