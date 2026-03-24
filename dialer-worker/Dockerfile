FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml pom.xml
COPY dialer-common/pom.xml dialer-common/pom.xml
COPY dialer-worker-core/pom.xml dialer-worker-core/pom.xml
COPY dialer-provider-asterisk/pom.xml dialer-provider-asterisk/pom.xml
COPY dialer-worker/pom.xml dialer-worker/pom.xml
COPY dialer-common/src dialer-common/src
COPY dialer-worker-core/src dialer-worker-core/src
COPY dialer-provider-asterisk/src dialer-provider-asterisk/src
COPY dialer-worker/src dialer-worker/src
RUN mvn -pl dialer-worker -am package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/dialer-worker/target/dialer-worker-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java","-jar","/app/app.jar"]
