package it.valeriovaudi.lab.reservationservice.web.routes

import it.valeriovaudi.lab.reservationservice.domain.repository.ReservationRepository
import it.valeriovaudi.lab.reservationservice.web.representation.ReservationRepresentation
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.router
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import java.net.URI

@Configuration
class ReservationRoutesConfig {

    @Bean
    fun reservationRoutes(@Value("\${baseServer:http://localhost:8080}") baseServer: String,
                          reservationRepository: ReservationRepository) =
            router {
                POST("/reservation") {
                    it.bodyToMono(ReservationRepresentation::class.java)
                            .flatMap { Mono.just(ReservationRepresentation.toDomain(reservationRepresentation = it)) }
                            .flatMap { reservationRepository.save(it).toMono() }
                            .flatMap { ServerResponse.created(URI("$baseServer/reservation/${it.reservationId}")).build() }

                }

                GET("/reservation/{reservationId}") {
                    reservationRepository.findOne(it.pathVariable("reservationId")).toMono()
                            .flatMap { Mono.just(ReservationRepresentation.toRepresentation(it)) }
                            .flatMap { ok().body(BodyInserters.fromObject(it)) }
                }

                DELETE("/reservation/{reservationId}") {
                    reservationRepository.delete(it.pathVariable("reservationId")).toMono()
                            .then(noContent().build())
                }
            }
}