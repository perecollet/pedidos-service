FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY src src
RUN ./mvnw -q package -Dmaven.test.skip=true

FROM eclipse-temurin:17-jre
RUN useradd --system pedidos
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
USER pedidos
ENTRYPOINT ["java", "-jar", "app.jar"]
