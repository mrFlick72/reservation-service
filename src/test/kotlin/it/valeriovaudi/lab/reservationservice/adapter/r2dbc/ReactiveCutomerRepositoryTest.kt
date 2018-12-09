package it.valeriovaudi.lab.reservationservice.adapter.r2dbc

import io.r2dbc.client.R2dbc
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration
import io.r2dbc.postgresql.PostgresqlConnectionFactory
import it.valeriovaudi.lab.reservationservice.domain.model.Customer
import org.junit.Test
import reactor.core.publisher.toMono
import java.util.*

class ReactiveCutomerRepositoryTest {

    val r2dbc = R2dbc(PostgresqlConnectionFactory(PostgresqlConnectionConfiguration.builder()
            .host("localhost")
            .database("reservation")
            .username("root")
            .password("root")
            .build()))
    val reactiveCutomerRepository = ReactiveCutomerRepository(r2dbc)

    @Test
    fun `save a customer`() {
        val collectList = r2dbc.inTransaction { handle ->
            reactiveCutomerRepository.save2(Customer(UUID.randomUUID().toString(), "Valerio1", "Vaudi"), handle).toMono()
                    .then(reactiveCutomerRepository.save2(Customer(UUID.randomUUID().toString(), "Valerio2", "Vaudi"), handle).toMono())
                    .then(reactiveCutomerRepository.save2(Customer(UUID.randomUUID().toString(), "Valerio3", "Vaudi"), handle).toMono())
                    .then(reactiveCutomerRepository.save2(Customer(UUID.randomUUID().toString(), "Valerio4", "Vaudi"), handle).toMono())
                    .then(reactiveCutomerRepository.save2(Customer(UUID.randomUUID().toString(), "Valerio5", "Vaudi"), handle).toMono())
                    .then().map { handle.close() }
        }.collectList().block()
        println(collectList)
    }

    @Test
    fun `bare metal scenario`() {
        r2dbc.inTransaction { handle ->
            handle.execute("INSERT INTO customer (RESERVATION_ID, FIRST_NAME, LAST_NAME) VALUES ($1, $2, $3)", UUID.randomUUID().toString(), "Valerio1", "Vaud")
                    .then(handle.execute("INSERT INTO customer (RESERVATION_ID, FIRST_NAME, LAST_NAME) VALUES ($1, $2, $3)", UUID.randomUUID().toString(), "Valerio2", "Vaud").toMono())
                    .then(handle.execute("INSERT INTO customer (RESERVATION_ID, FIRST_NAME, LAST_NAME) VALUES ($1, $2, $3)", UUID.randomUUID().toString(), "Valerio3", "Vaud").toMono())
                    .then(handle.execute("INSERT INTO customer (RESERVATION_ID, FIRST_NAME, LAST_NAME) VALUES ($1, $2, $3)", UUID.randomUUID().toString(), "Valerio4", "Vaud").toMono())
        }.collectList().block()
    }
}