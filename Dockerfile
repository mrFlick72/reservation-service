FROM openjdk:11
COPY target/reservation-service.jar /usr/reservation-service
WORKDIR /usr/src/reservation-service

ENTRYPOINT ["java", "jar", "reservation-service.jar"]