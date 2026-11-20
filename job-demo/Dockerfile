FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml ./
COPY job-admin/pom.xml job-admin/
COPY job-executor-starter/pom.xml job-executor-starter/
COPY job-demo/pom.xml job-demo/
COPY job-admin/src job-admin/src
COPY job-executor-starter/src job-executor-starter/src
COPY job-demo/src job-demo/src
RUN mvn -f pom.xml -pl job-demo -am package -DskipTests -q

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /build/job-demo/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
