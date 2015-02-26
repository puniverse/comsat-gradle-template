# Comsat Gradle Template

Comsat Gradle template project for both Dropwizard embedded and Tomcat standalone Java 1.7+ applications showing how to setup servlets, Jersey (client and server), JDBC, Metrics and Apache HTTP Client. Tomcat 7/8 standalone deployments are managed through Cargo.

```
./gradlew -Penv=dropwizard run # CTRL+C to stop
./gradlew -Penv=tomcat7 clean test
./gradlew -Penv=tomcat7 cargoRunLocal # CTRL+C to stop
./gradlew -Penv=tomcat8 clean test
./gradlew -Penv=tomcat8 cargoRunLocal # CTRL+C to stop
```
