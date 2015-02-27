# Up & running with Comsat

## General intro

- Hi, I'm Fabio from Parallel Universe and I'll show how to get started with Comsat.
- Comsat is an integration framework for the JVM that tailors the performance and scalability of Quasar to the daily needs of application developers.
- It does so by either integrating popular pre-existing Java technologies or by introducing new and innovative APIs.

## Screen-cast plan

- An easy way to bootstrap your Comsat project is using either the Gradle template (https://github.com/puniverse/comsat-gradle-template) or the Maven archetype (https://github.com/puniverse/comsat-mvn-archetype). We're going to cover it shortly but if you need more detail this blog post (http://blog.paralleluniverse.co/2014/05/15/modern-java-pt3/) covers the Dropwizard part, while this blog post (http://blog.paralleluniverse.co/2015/01/07/comsat-servlet-container/) explains the Tomcat part.
- In fact, Comsat supports both embedded and standalone application servers, so the template includes both a Dropwizard application and a traditional servlet-based one running on Tomcat 7 and 8.
- Today, I'll start with the non-Comsat version of the Gradle template, available in the `without-comsat` branch, and I'll show how to enhance it with Comsat support, turning it into the full-fledged one available in the `master` branch.
- We'll see how to port both the Dropwizard application and the servlet-based one to Comsat, starting with Dropwizard. The reference for this portingcan be found at https://github.com/puniverse/comsat-gradle-template/compare/without-comsat?expand=1.

## Getting started

- First of all, let's browse to the Gradle template (https://github.com/puniverse/comsat-gradle-template) and let's choose the `without-comsat` branch.
- Since we're interested in just bootstrapping our own project, and not working on the template itself, we can use GitHub's Subversion support to export the template without any version control information. We'll export the `without-comsat` branch:

```
svn export https://github.com/puniverse/comsat-gradle-template/branches/without-comsat my-comsat-prj
```

- Aright, now let's open the Gradle project with IntelliJ Idea.
- Let's have a look around:
  - It is a pretty standard Gradle Java Web project with the usual layout.
  - Then there are a `tomcat` and a `lib` directory and a bunch of configuration files we'll cover shortly.
  - Let's inspect quickly the main Gradle build script:
    - It tmports the Cargo plugin, which we'll use to start the Tomcat servlet containers and to run integration tests against them.
    - It imports a bunch of configuration properties, including the ones that are customizable by the user in `gradle/user-props.gradle`.
    - It defines the `provided` configuration, which is not built-in in Gradle and which we'll use for compile-time-only dependencies.
    - It sets up repositories, dependencies and asks for explicit version conflict resolution, which is why you can see the `force` lines.       - It adds some more information to be printed when running tests.
    - Finally, it checks the `env` property to include the rest of the build configuration, falling back on `tomcat8` if missing. You can find the `dropwizard`, `tomcat7` and `tomcat8` build scripts in the `gradle` subdirectory.

## The `dropwizard` embedded appserver environment

- Dropwizard is a very nice framework for modern, embedded Java web applications. It integrates the Jetty embedded application server with several popular specialized Java APIs and frameworks, offering a full-fledged integrated webapp development stack.
- In particular we're going to use Dropwizard's Jersey, JDBI and Metrics support.
  - Jersey is the most popular implementation of JAX-RS, a popular annotation-enabled Java API to write ReSTful web services.
  - JDBI is a slick and light SQL convenience library for Java.
  - Metrics is a toolkit to measure and report the behaviour and performance of critical code sections in production.
- In addition, we're going to use Netflix's Feign, a convenient contract-based HTTP client with several backends, as well as Dagger, a compact dependency injection engine with static check support. The SQL backend will be H2 in embedded memory mode.
- Please note that our application fits a single Java class file, `Main.java`.
- Let's run the application with `./gradlew -Penv=dropwizard run` and let's check a few URLs:
  - http://localhost:8080/hello-world
  - http://localhost:8080/consumer
  - http://localhost:8080/db/item/1
  - http://localhost:8080/db/all
- Ok, all is good.

## Porting the Dropwizard embedded webapp to Comsat

- Since we're going to use JDK8, which yields even better performance, we'll be using JDK8-optimised version of some Quasar and Comsat artifacts, so let's add the `ext.classifier      = ':jdk8'` property to `gradle/user-props.gradle`, we're going to use it shortly. For the same reason you should use a locally built `comsat-tomcat-loader-jdk8` artifact because there's a bug in the released one that has already been fixed and will be released soon.
- Let's first review our dependencies in `build.gradle`:
  - We'll introduce the `quasar` configuration to get a reference to the Quasar agent JAR.
  - Let's replace the Jersey server with the Quasar-enabled one: "org.glassfish.jersey.containers:jersey-container-servlet:$jerseyVer" -> `compile "co.paralleluniverse:comsat-jersey-server:$comsatVer"`
  - We can also remove the explicit dependence to Apache HTTP Client because Comsat will bring it in: - `compile "org.apache.httpcomponents:httpclient:$httpClientVer"`
  - Let's add the other Quasar and Comsat dependencies, we're going to use Quasar and Comsat JDBC as well: + `compile "co.paralleluniverse:quasar-core:$quasarVer"` + `compile "co.paralleluniverse:comsat-jdbc:$comsatVer"`
  - And let's add their versions to `gradle/props.gradle`: + `ext.comsatVer       = '0.3.0'` + `ext.quasarVer       = '0.6.2'`.
  - [MAYBE NOT NEEDED] We'll add a `provided` dependency on the `comsat-tomcat-loader`: + `provided "co.paralleluniverse:comsat-tomcat-loader:${comsatVer}${classifier}"`
  - We'll add a `quasar` dependency on the `quasar-agent` so we can reference it later: + `quasar "co.paralleluniverse:quasar-core:${quasarVer}${classifier}@jar"`
- Now let's update the Dropwizard-specific `dropwizard.gradle`:
  - We'll just add the Quasar agent to the JVM args: + `"-javaagent:${configurations.quasar.singleFile}",`
- Time to port the code now, just few changes are needed:
  - The base class for a Dropwizard application needs to become `FiberApplication` and its main method needs to override `fiberRun`.
  - The JDBI factory will become `FiberDBIFactory`.
  - Let's make our JAX-RS resource methods suspendable by adding `@Suspendable` to `sayHello` and `consume`.
  - Let's make our DAO methods suspendable by adding `throws SuspendExecution` to `add`, `find` and `all`.
  - Finally, let's update the Dropwizard configuration file to tune the server so it can take advantage of fibers to serve more concurrent requests and to use the fiber-enabled JDBC driver:

```
server:
  maxThreads: 200
  minThreads: 200
  maxQueuedRequests: 9999
  requestLog:
    appenders: []

template: Hello, %s!
defaultName: Stranger

database:
  driverClass: co.paralleluniverse.fibers.jdbc.FiberDriver
  url: jdbc:fiber:h2:mem:test
  user: u
  password: p
```

- If we run the application now, we'll get a verification exception because Quasar will find that some methods still miss instrumentation. Since I have already been through that check, I'll spare some time now and will directly create the needed files:
  - `src/main/resources/META-INF/suspendable-supers` for suspendable interfaces, we only have the DAO:
```
testgrp.dw.Main$ModernDAO.insert
testgrp.dw.Main$ModernDAO.findById
testgrp.dw.Main$ModernDAO.all
```
  - We also need `src/main/resources/META-INF/suspendables` because JDBI will generate a dynamic proxy for our DAO implementation at runtime, and we need to make its methods suspendable too which we can't do through annotations or `throws` clauses because it's dynamically generated code:
```
testgrp.dw.$Proxy41.insert
testgrp.dw.$Proxy41.all
testgrp.dw.$Proxy41.findById
```
- Let's run the application again with `./gradlew -Penv=dropwizard run` and let's check a few URLs:
  - http://localhost:8080/hello-world
  - http://localhost:8080/consumer
  - http://localhost:8080/db/item/1
  - http://localhost:8080/db/all
- Great, porting done!

## The `tomcat7` and `tomcat8` standalone servlet container environments

- The Tomcat environments are pretty standard servlet container environments. We're going to use normal servlet, JAX-RS and JDBC APIs.
- If you take a glance at `gradle/servlet-standalone.gradle` you'll see we use a customized Cargo deployment because we need to configure the server with a global datasource, so we'll deploy a customized version of the main Tomcat configuration file `server.xml` and we'll include the H2 database driver in `commons/lib`.
- Let's run Cargo-based integration tests:
```
./gradlew -Penv=tomcat7 clean test
./gradlew -Penv=tomcat8 clean test
```
- All green!

## Porting servlet webapp to Comsat

- Let's review the `servlet-standalone.gradle` build file:
  - Let's add the `comsatTomcatLoader` and `comsatJdbc` configurations, which we need to add another couple of jars to `commons/lib`, that is the Quasar Tomcat classloader we use instead of the agent and the Comsat JDBC wrapping driver.
  - We also add those to the dependencies:

```
comsatTomcatLoader "co.paralleluniverse:comsat-tomcat-loader:${comsatVer}${classifier}@jar"
comsatJdbc "co.paralleluniverse:comsat-jdbc:$comsatVer@jar"
```

  - We might want to write fiber-enabled integration tests as well, so let's add the Quasar agent to them: + `"-javaagent:${configurations.quasar.singleFile}",` at the beginning of `jvmArgs`.
- We'll now tell cargo to deploy to commons/lib`` our additional drivers both for Tomcat 7 and 8 by updating the `gradle/tomcat7.gradle` and `gradle/tomcat8.gradle` files with those additions:

```
 file {
    file = file("${configurations.comsatTomcatLoader.singleFile}")
    toDir = file('/common/lib')
}
file {
    file = file("${configurations.comsatJdbc.singleFile}")
    toDir = file('/common/lib')
}
```

- Let's now review the code:
  - We'll turn the  a `JDBCPersistenceServieImpl` into a `FiberJDBCPersistenceServiceImpl` by simply declaring additional `InterruptedException` and `SuspendExecution` in DAO methods `store` and `checkRO`. Let's point to Comsat's `fiberds` datasource, too.
  - Let's sync the DAO interface too, `PersistenceService`, so we don't have to create a `suspendable-supers` file, but this is always an option if you don't want to touch your interfaces (or if you can't).
  - Let's bind the new service implementation to dependency injection by updating `DependencyBinder` too.
  - Our `Resource` can be updated just as easily by declaring additional `InterruptedException` and `SuspendExecution` in resource methods `hello`, `checkRO` and `store`.
  - Upon webapp boostrap, in `ServletContextSetup`, if we're going to use programmatic configuration we need to reference the Comsat Jersey servlet instead of the standard one: `javax.servlet.ServletRegistration.Dynamic fiber = sc.addServlet("fiber", co.paralleluniverse.fibers.jersey.ServletContainer.class);` and let's update the mapping too: `fiber.addMapping("/fiber-jaxrs/*");`
  - Our plain servlet component, `TestThreadServlet`, will become `TestFiberServlet` and we'll replace the thread sleep with a more efficient fiber sleep.
  - The Tomcat webapp context `src/main/webapp/META-INF/context.xml` will require two additions:
    - First of all, Quasar instrumentation in the form of a dedicated Tomcat classloader:

```
    <Loader loaderClass="co.paralleluniverse.comsat.tomcat.QuasarWebAppClassLoader" />
```

    - Secondly, we'll wrap the global JDBC datasource with a fiber-enabled one, so let's rename the `ResourceLink` to `links` and let's add the Comsat wrapper:

```
<!--wrap the linked global db resource by fiber wrapper-->
<Resource name="jdbc/fiberds" auth="Container"
          type="javax.sql.DataSource"
          rawDataSource="jdbc/linkds"
          threadsCount="10"
          url="fiber"
          factory="co.paralleluniverse.fibers.jdbc.FiberDataSourceFactory"
        />
```

  - We'll need to update the `web.xml` descriptor too, in case we use descriptor-based setup, since we renamed quite few things and we need to use the Comsat Jersey implementation, which also requires async support to be enabled by the container:

```
    <display-name>fiber</display-name>
    <servlet-name>fiber</servlet-name>
    <servlet-class>co.paralleluniverse.fibers.jersey.ServletContainer</servlet-class>
    <!-- Support async (needed by fiber-blocking) -->
    <async-supported>true</async-supported>
```

```
<servlet-mapping>		     <servlet-mapping>
    <servlet-name>fiber</servlet-name>		+        <servlet-name>servlet</servlet-name>
    <url-pattern>/fiber-jaxrs/*</url-pattern>		+        <url-pattern>/thread-jaxrs/*</url-pattern>
</servlet-mapping>
```

```
<res-ref-name>jdbc/fiberds</res-ref-name>
```
  - Finally, since we changed servlet mappings for clarity, let's update the URLs used by integration tests in `WebAppIT` from `/thread`-something to `/fiber-`something.
- Let's now re-run out integration tests:
```
./gradlew -Penv=tomcat7 clean test
./gradlew -Penv=tomcat8 clean test
```
- All green again! Porting done.

## Conclusion

- To sum it all up, porting a web application to Comsat is not at all difficult nor a long task. With little effort you get the amazing efficienty and scalability of Quasar Fibers in your usual environment.
- I think that the Gradle and Maven templates will be useful to you, and hopefully this screen-cast too, which I hope you enjoyed.
- Have fun and meet again soon!