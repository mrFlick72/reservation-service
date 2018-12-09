package it.valeriovaudi.lab.reservationservice.domain.repository

import it.valeriovaudi.lab.reservationservice.domain.model.Customer
import org.reactivestreams.Publisher

interface CutomerRepository {
    fun save(customer: Customer): Publisher<Customer>

    fun findAll(): Publisher<Customer>
}