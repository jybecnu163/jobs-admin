FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml ./
COPY job-admin/pom.xml job-admin/
COPY job-executor-starter/pom.xml job-executor-starter/
COPY job-admin/src job-admin/src
COPY job-executor-starter/src job-executor-starter/src
RUN mvn -f pom.xml -pl job-admin -am package -DskipTests -q

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /build/job-admin/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
