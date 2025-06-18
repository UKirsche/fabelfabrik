# Build Stage
FROM quay.io/quarkus/ubi-quarkus-mandrel-builder-image:23.1.7.0-Final-java21 AS build
WORKDIR /usr/src/app
COPY . .
RUN ./mvnw package -DskipTests

# Run Stage (JVM Runner)
FROM eclipse-temurin:21-jre
WORKDIR /work/
COPY --from=build /usr/src/app/target/quarkus-app/ ./

EXPOSE 8080

CMD ["java", "-jar", "quarkus-run.jar"]
