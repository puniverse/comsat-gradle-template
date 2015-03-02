# Up & running with Comsat


## General intro

(Browser in http://www.paralleluniverse.co/comsat/)

- Hi, I'm Fabio from Parallel Universe and I'll show how to get started with Comsat.

- Comsat is an integration framework for the JVM that tailors the performance and scalability of Quasar to the daily needs of application developers.

- Comsat either integrates pre-existing Java technologies or introduces innovative APIs, like Web Actors.

- With Comsat and Quasar you get the same performance and scalability as all the asynchronous frameworks out there, but you can still use a simple, sequential and familiar coding style rather than de-structuring your program into a maze of callbacks.

- Plus, you can still use the libraries and tools you already know.


## Screen-cast plan

(Browser prepared for:
- https://github.com/puniverse/comsat-gradle-template
- https://github.com/puniverse/comsat-mvn-archetype
- http://blog.paralleluniverse.co/2014/05/15/modern-java-pt3/
- http://blog.paralleluniverse.co/2015/01/07/comsat-servlet-container/
)

- An easy way to bootstrap your Comsat project is using either the Gradle template (https://github.com/puniverse/comsat-gradle-template) or the Maven archetype (https://github.com/puniverse/comsat-mvn-archetype).

- Comsat supports both embedded and standalone application servers, so the template includes both a Dropwizard application and a traditional servlet-based one running on Tomcat 7 and 8.

- We're going to cover the Gradle template shortly but if you need more detail this blog post (http://blog.paralleluniverse.co/2014/05/15/modern-java-pt3/) covers the Dropwizard part, while this blog post (http://blog.paralleluniverse.co/2015/01/07/comsat-servlet-container/) explains the Tomcat part.

- Today, I'll start with the non-Comsat version of the Gradle template, available in the `without-comsat` branch, and I'll show how to enhance it with Comsat support, turning it into the full-fledged one available in the `master` branch.

- We'll see how to port both the Dropwizard application and the servlet-based one to Comsat, starting with Dropwizard.


## Getting started - 1

- First of all, let's browse to the Gradle template (https://github.com/puniverse/comsat-gradle-template) and let's choose the `without-comsat` branch.

- Since we're interested in just bootstrapping our own project, and not working on the template itself, we can use GitHub's Subversion support to export the template without any version control information.

- We can SVN-export the whole repository including both the `master` and the `without-comsat` branch, so that we can diff them to see what the differences are:

```
svn export https://github.com/puniverse/comsat-gradle-template
```


## Getting started - 2

- Aright, now let's open the `without-comsat` Gradle project with IntelliJ Idea.

- Let's have a look around:

  - It is a pretty standard Gradle Java Web project with the usual layout.

  - Let's inspect quickly the main Gradle build script:

    - It imports the Cargo plugin, which we'll use to start the Tomcat servlet containers and to run integration tests against them.

    - It imports a bunch of configuration properties, including the ones that are customizable by the user in `gradle/user-props.gradle`.

    - It defines the `provided` configuration, which is not built-in in Gradle and which we'll use for compile-time-only dependencies.

    - It sets up repositories, dependencies and asks for explicit version conflict resolution, which is why you can see the `force` lines.

      - It adds some more information to be printed when running tests.

    - Finally, it checks the `env` property to include the rest of the build configuration, falling back on `tomcat8` if missing. You can find the `dropwizard`, `tomcat7` and `tomcat8` build scripts in the `gradle` subdirectory.


## The `dropwizard` embedded appserver environment

(Prepare browsers with navigation to the frameworks in advance:
- http://dropwizard.io/
- https://jersey.java.net/
- http://jdbi.org/
- https://dropwizard.github.io/metrics/3.1.0/
- https://github.com/Netflix/feign
- http://square.github.io/dagger/
- http://h2database.com/html/main.html
)

- Dropwizard is a very nice framework for modern, embedded Java web applications. It integrates the Jetty embedded application server with several popular specialized Java APIs and frameworks, offering a full-fledged integrated webapp development stack.

- In particular we're going to use Dropwizard's Jersey, JDBI and Metrics support.

  - Jersey is the most popular implementation of JAX-RS, a popular annotation-enabled Java API to write ReSTful web services.

  - JDBI is a slick and light SQL convenience library for Java.

  - Metrics is a toolkit to measure and report the behaviour and performance of critical code sections in production.

- In addition, we're going to use Netflix's Feign, a convenient contract-based HTTP client with several backends, as well as Dagger, a compact dependency injection engine with static check support. The SQL backend will be H2 in embedded memory mode.

- Please note that our application fits a single Java class file, `Main.java`.

- Let's run the pre-Comsat application with `./gradlew -Penv=dropwizard run` and let's check a few URLs:
  - http://localhost:8080/hello-world
  - http://localhost:8080/consumer
  - http://localhost:8080/db/item/1
  - http://localhost:8080/db/all

- Ok, everything works as expected.


## DIR DIFF - Porting the Dropwizard embedded webapp to Comsat

(Prepare simplified versions for diff, open in IntelliJ idea with diff dialog and set prj source level to 1.8)

- Since we're going to use JDK8, which yields even better performance, we'll be using JDK8-optimised version of some Quasar and Comsat artifacts, so let's add the `ext.classifier = ':jdk8'` property to `gradle/user-props.gradle`, we're going to use it shortly.

  - And let's also add Quasar and Comsat versions we'll be using in `gradle/props.gradle`.

- Let's review our dependencies in `build.gradle`:

  - We'll introduce the `quasar` configuration to reference the Quasar agent JAR later on.

  - We'll replace `jersey-container-servlet` with the Comsat-enabled jersey integration.

  - We can also remove the explicit dependency to Apache HTTP Client because Comsat already depends on it.

  - Let's add the Comsat-Dropwizard and the Comsat-JDBI integration, while removing the Dropwizard one.
    - Don't worry too much about the exclusions, they're there only to simplify the build script for the IDE and to allow both the Dropwizard and Tomcat parts to compile together as they depend on different Jersey version. In fact we'll override them in the Dropwizard build script.

  - Let's add the other Quasar and Comsat dependencies, we're going to use Quasar and Comsat JDBC as well.

  - We'll add a `quasar`-type dependency on the `quasar-agent` so we can reference it later.

- Now let's update the Dropwizard-specific `dropwizard.gradle`:

  - We'll add the Quasar agent to the JVM args.

  - We'll override the Dropwizard dependencies, removing exclusions so that the Dropwizard environment runs fine.

- Time to port the code now, just few changes are needed:

  - The base class for a Dropwizard application needs to become `FiberApplication` and its main method needs to override `fiberRun`.

  - The JDBI factory will become `FiberDBIFactory`.

  - Let's make our JAX-RS resource methods suspendable by adding `@Suspendable` to `sayHello` and `consume`.

  - We'll also turn the `Thread.sleep` into a much more efficient `Fiber.sleep` now that we can use fibers.

  - Let's make our DAO methods suspendable by adding `throws SuspendExecution` to `add`, `find` and `all`.

  - Finally, let's update the Dropwizard configuration file to tune the server so it can take advantage of fibers to serve more concurrent requests and to use the fiber-enabled JDBC driver:

- If we run the application now, we'll get a verification exception because Quasar will find that some methods still miss instrumentation. Since I have already been through that check, I'll spare some time now and will directly create the needed files:

  - `src/main/resources/META-INF/suspendable-supers` for suspendable interfaces, we only have the DAO:

  - We also need `src/main/resources/META-INF/suspendables` because JDBI will generate a dynamic proxy for our DAO implementation at runtime, and we need to make its methods suspendable too. We can't do through annotations or `throws` clauses because it's dynamically generated code:

- Let's run the application again with `./gradlew -Penv=dropwizard run` and let's check a few URLs:
  - http://localhost:8080/hello-world
  - http://localhost:8080/consumer
  - http://localhost:8080/db/item/1
  - http://localhost:8080/db/all

- Great, porting done!


## The `tomcat7` and `tomcat8` standalone servlet container environments

- The Tomcat environments are pretty standard servlet container environments. We're going to use normal servlet, JAX-RS and JDBC APIs.

- If you take a glance at `gradle/servlet-standalone.gradle` you'll see we use a customized Cargo deployment because we need to configure the server with a global data source.

- We deploy a customized version of the main Tomcat configuration file `server.xml` and we include the H2 database driver in `commons/lib`.

- Let's run Cargo-based integration tests:

```
./gradlew -Penv=tomcat7 clean test
./gradlew -Penv=tomcat8 clean test
```

- All green!


## DIR DIFF - Porting servlet webapp to Comsat

- Let's review the `servlet-standalone.gradle` build file:

  - Let's add the `comsatTomcatLoader` and `comsatJdbc` configurations, which we need to add another couple of jars to `commons/lib`, that is the Quasar Tomcat class loader we use instead of the agent and the Comsat JDBC wrapping driver.

  - We also add those to the dependencies:

  - We might want to write fiber-enabled integration tests as well, so let's add the Quasar agent to them: + `"-javaagent:${configurations.quasar.singleFile}",` at the beginning of `jvmArgs`.

  - Since we're not using the agent but the Tomcat class-loader, we tell Quasar not to complain about it not being running.

- We'll now tell cargo to deploy to `commons/lib` our additional drivers both for Tomcat 7 and 8 by updating the `gradle/tomcat7.gradle` and `gradle/tomcat8.gradle` files with those additions:

- Let's now review the code:

  - We'll change the `JDBCPersistenceServiceImpl` into a `FiberJDBCPersistenceServiceImpl` by simply declaring additional `InterruptedException` and `SuspendExecution` in DAO methods `store` and `checkRO`. Let's point to Comsat's `fiberds` datasource, too.

  - We're not usign that one but let's also similarly change the mock `ThreadSleepPersistenceServiceImpl` into a `FiberSleepPersistenceServiceImpl`.

  - Let's sync the DAO interface too, `PersistenceService`, so we don't have to create a `suspendable-supers` file, but this is always an option if you don't want to touch your interfaces (or if you can't).

  - Let's bind the new service implementation to dependency injection by updating `DependencyBinder` too.

  - Our `Resource` can be updated just as easily by declaring additional `InterruptedException` and `SuspendExecution` in resource methods `hello`, `checkRO` and `store`.

  - Upon webapp bootstrap, in `ServletContextSetup`, if we're going to use programmatic configuration we need to reference the Comsat Jersey servlet instead of the standard one: `javax.servlet.ServletRegistration.Dynamic fiber = sc.addServlet("fiber", co.paralleluniverse.fibers.jersey.ServletContainer.class);` and let's update the mapping too: `fiber.addMapping("/fiber-jaxrs/*");`

  - Our plain servlet component, `TestThreadServlet`, will become `TestFiberServlet` and we'll replace the thread sleep with a more efficient fiber sleep.

  - The Tomcat webapp context `src/main/webapp/META-INF/context.xml` will require two additions:

    - First of all, Quasar instrumentation in the form of a dedicated Tomcat classloader:

  - We'll need to update the `web.xml` descriptor too, in case we use descriptor-based setup, since we renamed quite few things and we need to use the Comsat Jersey implementation, which also requires async support to be enabled by the container:

  - Finally, since we changed servlet mappings for clarity, let's update the URLs used by integration tests in `WebAppIT` from `/thread`-something to `/fiber-`something.

- Let's now re-run out integration tests:

```
./gradlew -Penv=tomcat7 clean test
./gradlew -Penv=tomcat8 clean test
```

- All green again! Porting done.


## Conclusion

- To sum it all up, porting a web application to Comsat is not at all difficult nor a long task. With little effort you get the amazing efficiency and scalability of Quasar Fibers in your usual environment.

- I think that the Gradle and Maven templates will be useful to you, and hopefully this screen-cast too, which I hope you enjoyed.

- Have fun and meet again soon!