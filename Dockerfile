# Build Stage
FROM quay.io/quarkus/ubi-quarkus-mandrel-builder-image:23.1-java17 AS build
WORKDIR /usr/src/app
COPY . .
RUN ./mvnw package -DskipTests

# Run Stage (JVM Runner)
FROM quay.io/quarkus/quarkus-micro-image:3.2
WORKDIR /work/
COPY --from=build /usr/src/app/target/quarkus-app/ ./

EXPOSE 8080

CMD ["java", "-jar", "quarkus-run.jar"]