package it.valeriovaudi.lab.reservationservice.web.config

import io.r2dbc.postgresql.PostgresqlConnectionConfiguration
import io.r2dbc.postgresql.PostgresqlConnectionFactory
import io.r2dbc.spi.ConnectionFactory
import it.valeriovaudi.lab.reservationservice.adapter.r2dbc.ReactiveCutomerRepository
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.r2dbc.function.TransactionalDatabaseClient

@Configuration
@EnableConfigurationProperties(value = [R2dbcCongfig::class])
class RepositoryConfig {

    @Bean
    fun postgresqlConnectionFactory() =
            PostgresqlConnectionFactory(PostgresqlConnectionConfiguration.builder()
                    .host("localhost")
                    .database("reservation")
                    .username("root")
                    .password("root")
                    .build())


    @Bean
    fun databaseClient(postgresqlConnectionFactory: ConnectionFactory) =
            TransactionalDatabaseClient.create(postgresqlConnectionFactory)

    @Bean
    fun reactiveCutomerRepository(databaseClient: TransactionalDatabaseClient) =
            ReactiveCutomerRepository(databaseClient)

}