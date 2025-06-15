# fabelfabrik

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

## Running the application in dev mode

### Starting MongoDB with Docker Compose

Before running the application, you need to start MongoDB using Docker Compose:

```shell script
docker-compose up -d
```

This will start a MongoDB instance on port 27017 with a database named "storydb".

To stop the MongoDB container when you're done, run:

```shell script
docker-compose down
```

### Running the application

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

## Packaging and running the application

The application can be packaged using:

```shell script
./mvnw package
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using:

```shell script
./mvnw package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/fabelfabrik-1.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/maven-tooling>.

## Related Guides

- RESTEasy Classic's REST Client JSON-B ([guide](https://quarkus.io/guides/resteasy-client)): JSON-B serialization
  support for the REST client
- MongoDB client ([guide](https://quarkus.io/guides/mongodb)): Connect to MongoDB in either imperative or reactive style
- MongoDB with Panache ([guide](https://quarkus.io/guides/mongodb-panache)): Simplify your persistence code for MongoDB
  via the active record or the repository pattern
- REST JSON-B ([guide](https://quarkus.io/guides/rest#json-serialisation)): JSON-B serialization support for Quarkus
  REST. This extension is not compatible with the quarkus-resteasy extension, or any of the extensions that depend on
  it.
- SmallRye OpenAPI ([guide](https://quarkus.io/guides/openapi-swaggerui)): Document your REST APIs with OpenAPI - comes
  with Swagger UI

## Provided Code

### REST

Easily start your REST Web Services

[Related guide section...](https://quarkus.io/guides/getting-started-reactive#reactive-jax-rs-resources)
