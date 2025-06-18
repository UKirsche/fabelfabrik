# Build Stage
FROM quay.io/quarkus/ubi-quarkus-mandrel-builder-image:23.1.7.0-Final-java21 AS build
WORKDIR /usr/src/app
COPY . .
RUN mvn package -DskipTests

# Run Stage (JVM Runner)
FROM quay.io/quarkus/quarkus-micro-image:2.0-2025-06-15
WORKDIR /work/
COPY --from=build /usr/src/app/target/quarkus-app/ ./

EXPOSE 8080

CMD ["java", "-jar", "quarkus-run.jar"]