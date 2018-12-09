package it.valeriovaudi.lab.reservationservice.adapter.r2dbc

import io.r2dbc.client.Handle
import io.r2dbc.client.R2dbc
import it.valeriovaudi.lab.reservationservice.domain.model.Customer
import it.valeriovaudi.lab.reservationservice.domain.repository.CutomerRepository
import org.reactivestreams.Publisher
import reactor.core.publisher.Mono

class ReactiveCutomerRepository(private val r2dbc: R2dbc) : CutomerRepository {

    override fun findAll(): Publisher<Customer> =
            r2dbc.inTransaction { it.select("SELECT * FROM customer").mapRow { row -> Customer(row.get("RESERVATION_ID") as String, row.get("FIRST_NAME") as String, row.get("LAST_NAME") as String) } }

    override fun save(customer: Customer): Publisher<Customer> =
            r2dbc.inTransaction { it.execute("INSERT INTO customer (RESERVATION_ID, FIRST_NAME, LAST_NAME) VALUES ($1, $2, $3)", customer.reservationId, customer.firstName, customer.lastName) }
                    .flatMap { Mono.just(customer) }


    fun save2(customer: Customer, handle: Handle) =
            handle.execute("INSERT INTO customer (RESERVATION_ID, FIRST_NAME, LAST_NAME) VALUES ($1, $2, $3)", customer.reservationId, customer.firstName, customer.lastName)
                    .then(Mono.just(customer));

}