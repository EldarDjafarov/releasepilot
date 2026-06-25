FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN ./mvnw -q package -DskipTests 2>/dev/null || (apt-get install -y maven 2>/dev/null; mvn -q package -DskipTests)

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/releasepilot-*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
