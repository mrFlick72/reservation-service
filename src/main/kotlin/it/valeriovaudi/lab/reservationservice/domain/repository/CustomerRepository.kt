package it.valeriovaudi.lab.reservationservice.domain.repository

import it.valeriovaudi.lab.reservationservice.domain.model.Customer
import org.reactivestreams.Publisher

interface CustomerRepository {
    fun save(reservationId: String, customer: Customer): Publisher<Customer>

    fun find(reservationId: String) : Publisher<Customer>

    fun delete(reservationId: String) : Publisher<Void>
}