# Multi-stage build for backend - Use Maven with JDK 21 (Project uses Java 21)
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copy only the POM first to leverage Docker cache
COPY pom.xml .

# Download dependencies (this layer will be cached unless pom.xml changes)
RUN mvn dependency:go-offline

# Copy the rest of the source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Final runtime stage - Use JRE 21 for smaller image size
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.war app.war
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.war"]
