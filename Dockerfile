ARG MODULE

FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY pom.xml mvnw ./
COPY .mvn .mvn
COPY notiflow-contracts notiflow-contracts
COPY notiflow-api notiflow-api
COPY notiflow-worker notiflow-worker
ARG MODULE
RUN mvn --batch-mode -pl ${MODULE} -am package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
ARG MODULE
COPY --from=build /workspace/${MODULE}/target/${MODULE}-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
