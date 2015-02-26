# Gradle Template

Gradle template project for both Dropwizard embedded and Tomcat standalone Java 1.7+ applications showing how to setup servlets, Jersey (client and server), JDBC, Metrics and Apache HTTP Client. Tomcat 7/8 standalone deployments are managed through Cargo.

This is a port of the Comsat Maven Archetype at http://github.com/puniverse/comsat-maven-archetype (`without-comsat` branch).

## Getting started

Just edit `gradle/user-props.gradle`. You might want to add JVM arguments and system properties in `gradle/user-props.gradle` and `gradle/user-props.gradle`, then feel free to play with the code or give it a try as it stands.

A good overview can be found in http://blog.paralleluniverse.co/2015/01/07/comsat-servlet-container/.

Currently 3 profiles can be chosen through the `env` property: `dropwizard`, `tomcat7` and `tomcat8`. To run them:

```
./gradlew -Penv=dropwizard run # CTRL+C to stop
./gradlew -Penv=tomcat7 clean test
./gradlew -Penv=tomcat7 cargoRunLocal # CTRL+C to stop
./gradlew -Penv=tomcat8 clean test
./gradlew -Penv=tomcat8 cargoRunLocal # CTRL+C to stop
```
