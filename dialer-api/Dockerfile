FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml pom.xml
COPY dialer-common/pom.xml dialer-common/pom.xml
COPY dialer-api/pom.xml dialer-api/pom.xml
COPY dialer-common/src dialer-common/src
COPY dialer-api/src dialer-api/src
RUN mvn -pl dialer-api -am package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/dialer-api/target/dialer-api-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java","-jar","/app/app.jar"]
