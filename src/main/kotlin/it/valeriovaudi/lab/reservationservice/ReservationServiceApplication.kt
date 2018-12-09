package it.valeriovaudi.lab.reservationservice

import it.valeriovaudi.lab.reservationservice.domain.model.Customer
import it.valeriovaudi.lab.reservationservice.domain.repository.CutomerRepository
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.stereotype.Component
import reactor.core.publisher.toFlux
import reactor.core.publisher.toMono
import java.util.*

@SpringBootApplication
class ReservationServiceApplication

fun main(args: Array<String>) {
    runApplication<ReservationServiceApplication>(*args)
}

@Component
class Init(private val customerRepositoy: CutomerRepository) : ApplicationRunner {

    override fun run(args: ApplicationArguments?) {
        println("insert")
        customerRepositoy.save(Customer(UUID.randomUUID().toString(), "Valerio", "Vaudo"))
                .toMono().subscribe({ println(it)})

   /*     println("findAll")
        customerRepositoy.findAll().toFlux().subscribe({ println(it)})*/


    }

}