package it.valeriovaudi.lab.reservationservice.adapter.r2dbc

import it.valeriovaudi.lab.reservationservice.domain.model.Customer
import it.valeriovaudi.lab.reservationservice.domain.repository.CustomerRepository
import org.springframework.data.r2dbc.function.DatabaseClient
import org.springframework.data.r2dbc.function.TransactionalDatabaseClient
import reactor.core.publisher.Mono

class ReactiveCutomerRepository(private val databaseClient: TransactionalDatabaseClient) : CustomerRepository {

    override fun save(customer: Customer) =
            databaseClient.execute().sql("INSERT INTO customer (reservation_id, first_name, last_name) VALUES ($1, $2, $3)")
                    .bind("$1", customer.reservationId)
                    .bind("$2", customer.firstName)
                    .bind("$3", customer.lastName)
                    .fetch()
                    .rowsUpdated()
                    .flatMap { Mono.just(customer) }

    override fun find(reservationId: String) =
            databaseClient.execute().sql("Select * FROM customer WHERE reservation_id = $1")
                    .bind("$1", reservationId)
                    .exchange()
                    .flatMap { sqlRowMap ->
                        sqlRowMap.extract { t, u ->
                            Customer(t.get("reservation_id", String::class.java)!!,
                                    t.get("first_name", String::class.java)!!,
                                    t.get("last_name", String::class.java)!!)
                        }.one()
                    }
}
