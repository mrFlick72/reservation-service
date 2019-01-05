FROM openjdk:11

WORKDIR /usr/local/reservation-service/

COPY target/reservation-service.jar reservation-service.jar

CMD ["java", "-Xms512m", "-Xmx1g", "-jar", "reservation-service.jar"]