# Reservation-Service

This repo show a extremely cool thing: make a relation database access in non blocking io way embracing the reactive programming paradigm
starting from web layer to data access layer.

## Why consider no-blocking IO
Today the most common use case involve the classic one thread per req model. 
This model is typical for very famous web and application servers like: Apache httpd, Ngnix, Tomcat and so on. 
However when the load increase too much, this model can be not suitable. If the request per seconds are more then the available threads, we 
can see a decrease of performance or even a deny of service. Another approach that is emerging during these years, involve a totally different model. 
Instead to have one thread per request, projects and products like NodeJS, Netty, AKKA embrace the model of event loop and/or actor model. 
The problem here is that we have take care of never block our pipeline, due to we have only one thread per event loop. If we block our execution we will block anything.
However the history show that this model scale very well in high load use cases, indeed
we have more lightweight server, that consumes less resources and use those resources in a very more optimized way. 
A very beautiful metaphor here is the music. In a concert in order to have a good amplification system it is vital that all the, in the audio pipeline is of high quality. 
If we have good speakers and microphones but bad sound compressor, for example, the result will be less optimal then if we would have even a good sound compressor. 
Even in a no-blocking pipeline having all the pieces of the software using no-blocking paradigm is very important. We can't permit to have some blocking io software. 
Some framework like RXJava2, Reactror, Akka Stream and many other embrace reactive programming paradigm that provide a reactive programming paradigm that is very useful in order to 
compose software pieces.

## The evil: JDBC
Unfortunately JDBC do not embrace the no-blocking and reactive programming paradigm. This is a very big problem for use relation database in a reactive no-blocking pipeline.
Some projects like rxjava-jdbc, ADBA, R2DBC an so on, try to solve the problem of the blocking nature of JDBC. In this sample I show how to use R2DBC in a full reactive no-blocking io pipeline 
starting from the web layer(Spring WebFlux) to database.

## The stack of the sample

In this project I have used Spring Boot 2.1.1 with Kotlin as programming language, Spring WebFlux on the web layer and R2DBC on persistence layer, Postgres as database in a Hexagonal architecture.
In particular for the persistence I have experimented the newest Spring Data R2DBC, that thanks to a very elegant api, especially for the transactional management, provides a very elegant and clean api. 
Note in this example that tanks to the databaseClient of type TransactionalDatabaseClient and Spring Data R2DBC we can span the transaction 
across more repository in a more explicit way without annotation.
#### configuration
```kotlin
@Configuration
@EnableConfigurationProperties(value = [R2DBCCongfig::class])
class RepositoryConfig {

 ...
    @Bean
    fun databaseClient(postgresqlConnectionFactory: ConnectionFactory) =
            TransactionalDatabaseClient.create(postgresqlConnectionFactory)

}
```
#### sample code
```kotlin

}
  
class ReactiveReservationRepository(private val databaseClient: TransactionalDatabaseClient,
                                    private val customerRepository: CustomerRepository) : ReservationRepository {

    override fun findOne(reservationId: String): Publisher<Reservation> =
            databaseClient.inTransaction {
                customerRepository.find(reservationId).toMono()
                        .flatMap { customer ->
                            it.execute().sql("SELECT * FROM reservation WHERE reservation_id=$1")
                                    .bind("$1", reservationId)
                                    .exchange()
                                    .flatMap { sqlRowMap ->
                                        sqlRowMap.extract { t, u ->
                                            Reservation(t.get("reservation_id", String::class.java)!!,
                                                    t.get("restaurant_name", String::class.java)!!,
                                                    customer, t.get("date", LocalDateTime::class.java)!!)
                                        }.one()
                                    }
                        }
            }

....
}
          
```
## The web layer

For the web layer we have used Spring WebFlux and since that even the database layer now is a fully reactive programming with no blocking io model, we have 
the assurance that all the pipeline coherent and integrated like below simple and elegant!!!:

```kotlin
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
          
```
## Testing
The test class involve a local database Postgress, I do not provide H2 because 
I think that especially with native query it is very important to use the real database engine that we will use in production, 
of course tanks projects like TestContainers we can start a postgress database for our tests and test our code against a real database, 
the only problem here is the test speed. Unfortunately use testcontainers involve to download the immage, start a container and it can be time consuming.
The my strategy involve the usage on a docker compose because i feel it more confortable and configure the my test infrastructure can be more simple that 
configure by code the JUnit ClassRule. 

 **The sample code appear like below:** 
 
```kotlin
class ReactiveCustomerRepositoryTest {

    companion object {
        @ClassRule
        @JvmField
        val container: DockerComposeContainer<*> = DockerComposeContainer<Nothing>(File("src/test/resources/docker-compose.yml"))
                .withExposedService("postgres_1", 5432)

    }

    @Before
    fun setUp() {
        /**
             * I prefer do not use docker port redirect in order to prevents the port conflicts on container start,
             * imaging it on a concurrent test suite, the code below is necessary in order to get the host and port
             * that the docker runtime assign to the container
         * */
        val serviceHost = container.getServiceHost("postgres_1", 5432) 
        val servicePort = container.getServicePort("postgres_1", 5432)

        postgresqlConnectionFactory = PostgresqlConnectionFactory(PostgresqlConnectionConfiguration.builder()
                .host(serviceHost)
                .port(servicePort)
                .database("reservation")
                .username("root")
                .password("root")
                .build())

        databaseClient = TransactionalDatabaseClient.create(postgresqlConnectionFactory)
        reactiveCustomerRepository = ReactiveCustomerRepository(databaseClient)
        reactiveReservationRepository = ReactiveReservationRepository(databaseClient, reactiveCustomerRepository)

        r2dbc = R2dbc(postgresqlConnectionFactory)
    }

    @Test
    fun `save a customer not allowed tx rolbaked`() {
        val firstReservationId = UUID.randomUUID().toString()
        val secondReservationId = UUID.randomUUID().toString()
        val thirdReservationId = UUID.randomUUID().toString()

        val firstCustomer = newCustomer(prefix = "rolback", suffix = "1")
        val secondCustomer = newCustomer(prefix = "rolback", suffix = "2")
        val thirdCustomer = newCustomer(prefix = "rolback", suffix = "3")
        try {
            databaseClient.inTransaction {
                reactiveCustomerRepository.save(firstReservationId, firstCustomer)
                        .then(reactiveCustomerRepository.save(secondReservationId, secondCustomer))
                        .then(reactiveCustomerRepository.save(thirdReservationId, thirdCustomer))

                        .then(Mono.error<RuntimeException>({ RuntimeException() }))

                        .then()
            }.toMono().block(Duration.ofMinutes(1))
        } catch (e: Exception) {
        }

        Assert.assertTrue(findOneBy(firstReservationId)!!.size == 0)
        Assert.assertTrue(findOneBy(secondReservationId)!!.size == 0)
        Assert.assertTrue(findOneBy(thirdReservationId)!!.size == 0)
    }

    @Test
    fun `save a customer`() {

        val firstReservationId = UUID.randomUUID().toString()
        val secondReservationId = UUID.randomUUID().toString()
        val thirdReservationId = UUID.randomUUID().toString()


        val firstCustomer = newCustomer(prefix = "save", suffix = "1")
        val secondCustomer = newCustomer(prefix = "save", suffix = "2")
        val thirdCustomer = newCustomer(prefix = "save", suffix = "3")


        databaseClient.inTransaction {
            reactiveCustomerRepository.save(firstReservationId, firstCustomer)
                    .then(reactiveCustomerRepository.save(secondReservationId, secondCustomer))
                    .then(reactiveCustomerRepository.save(thirdReservationId, thirdCustomer))
                    .then()
        }.toMono().block(Duration.ofMinutes(1))

        Assert.assertTrue(findOneBy(firstReservationId)!!.size == 1)
        Assert.assertTrue(findOneBy(secondReservationId)!!.size == 1)
        Assert.assertTrue(findOneBy(thirdReservationId)!!.size == 1)
    }

....
}
          
```

**configuration of integration test:**


```kotlin
@DirtiesContext
@Import(RepoConfig::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner::class)
class ReservationRepresentationRoutesTest  {

    companion object {
        @ClassRule
        @JvmField
        val container: DockerComposeContainer<*> = DockerComposeContainer<Nothing>(File("src/test/resources/docker-compose.yml"))
                .withExposedService("postgres_1", 5432)

    }

    @Autowired
    private lateinit var webClient: WebTestClient

    @Autowired
    private lateinit var reactiveReservationRepository: ReactiveReservationRepository

    private val A_RESTAURANT_NAME = "A_RESTAURANT_NAME"
    private val FIRST_NAME = "FIRST_NAME"
    private val LAST_NAME = "LAST_NAME"
    private val A_DATE = LocalDateTime.of(2018, 1, 1, 10, 10)

    @Test
    fun `book a new reservation`() {
        val location = this.webClient.post()
                .uri("/reservation")
                .body(BodyInserters.fromObject(ReservationRepresentation(restaurantName = A_RESTAURANT_NAME,
                        customer = CustomerRepresentation(FIRST_NAME, LAST_NAME),
                        date = A_DATE)))
                .exchange()
                .expectStatus().isCreated
                .returnResult<Any>().responseHeaders.location

        println(location!!.extractId())
        Assert.assertNotNull(location.extractId())
    }

  ....
}

@Configuration
class RepoConfig {

    /**
     * I prefer do not use docker port redirect in order to prevents the port conflicts on container start,
     * imaging it on a concurrent test suite, the code below is necessary in order to get the host and port
     * that the docker runtime assign to the container
     * */
    @Bean
    @Primary // I force the usage of this bean instead of production bean in order to force the usage of postgress container for test case 
    fun connectionFactory(): PostgresqlConnectionFactory  =
            PostgresqlConnectionFactory(PostgresqlConnectionConfiguration.builder()
                    .host(ReservationRepresentationRoutesTest.container.getServiceHost("postgres_1", 5432))
                    .port(ReservationRepresentationRoutesTest.container.getServicePort("postgres_1", 5432))
                    .database("reservation")
                    .username("root")
                    .password("root")
                    .build())
}
```

**docker-compose:**

```yaml
version: "2"

services:
  postgres:
    image: postgres
    restart: always
    environment:
      POSTGRES_DB: reservation
      POSTGRES_USER: root
      POSTGRES_PASSWORD: root
    volumes:
      - ./schema.sql:/docker-entrypoint-initdb.d/schema.sql
      
```

## Conclusion

Use reactive programming with no blocking io can help our application to scale when the load is very high, of course 
not all application can benefits of this paradigm and the usage have to be understood, even if in many books we can read that this paradigm is more simple 
I disagree. I think that the correct load use case has the correct programming model have a reactive pipeline that remember a more functional programming 
style is cool but in some use case a little bit a over kill, the true way to do something unfortunately do not exist and it depends from many factors like:
team knowledge, load use case, scaling motivation and so on. In any case if the load is excpeted to increase and the resources usage is an important matter 
webflux, reactor and R2DBC can help us to build a fully reactive and no blocking io pipeline.

Only bad notice here is about the maturity of R2DBC. R2DBC unfortunately currently is not release and Spring Data R2DBC is in MILESTON version but I hope that these projects can 
reach the maturity soon.

## Refenrence
* [Spring Data R2DBC GitHub page](https://github.com/spring-projects/spring-data-r2dbc)
* [R2DBC web site](https://r2dbc.io/)
* [Testcontainers web site](https://www.testcontainers.org/)