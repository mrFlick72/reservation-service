package it.valeriovaudi.lab.reservationservice.web.routes

import it.valeriovaudi.lab.reservationservice.domain.repository.ReservationRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.server.router

import it.valeriovaudi.lab.reservationservice.web.representation.ReservationRepresentation
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import java.net.URI

@Configuration
class ReservationRoutes {

    @Bean
    fun reservationRoute(@Value("\${baseServer:http://localhost:8080}") baseServer: String,
                          reservationRepository: ReservationRepository) =
            router {
                POST("/reservation") {
                    it.bodyToMono(ReservationRepresentation::class.java)
                            .flatMap { Mono.just(ReservationRepresentation.toDomain(reservationRepresentation = it)) }
                            .flatMap { reservationRepository.save(it).toMono() }
                            .flatMap { ServerResponse.created(URI("$baseServer/reservation/${it.reservationId}")).build() }

                }

            }
}