package it.valeriovaudi.lab.reservationservice.web.config

import io.r2dbc.client.R2dbc
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration
import io.r2dbc.postgresql.PostgresqlConnectionFactory
import it.valeriovaudi.lab.reservationservice.adapter.r2dbc.ReactiveCutomerRepository
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(value = [R2dbcCongfig::class])
class RepositoryConfig {

    @Bean
    fun reactiveJdbc(r2dbcCongfig: R2dbcCongfig) : R2dbc {
        println(r2dbcCongfig)
        return R2dbc(PostgresqlConnectionFactory(PostgresqlConnectionConfiguration.builder()
                .host(r2dbcCongfig.host)
                .database(r2dbcCongfig.database)
                .username(r2dbcCongfig.username)
                .password(r2dbcCongfig.password)
                .build()))

    }

    @Bean
    fun reactiveCutomerRepository(reactiveJdbc: R2dbc) = ReactiveCutomerRepository(reactiveJdbc)
}