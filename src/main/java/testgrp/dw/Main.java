package testgrp.dw;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.fibers.dropwizard.FiberApplication;
import co.paralleluniverse.fibers.dropwizard.FiberDBIFactory;
import com.codahale.metrics.*;
import com.codahale.metrics.annotation.*;
import com.fasterxml.jackson.annotation.*;
import com.google.common.base.Optional;
import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;
import feign.Feign;
import feign.jackson.*;
import feign.jaxrs.*;
import io.dropwizard.db.*;
import io.dropwizard.setup.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import org.hibernate.validator.constraints.*;
import org.skife.jdbi.v2.*;
import org.skife.jdbi.v2.sqlobject.*;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.tweak.*;

public class Main extends FiberApplication<Main.JModernConfiguration> {
    public static void main(String[] args) throws Exception {
        new Main().run(new String[]{"server", System.getProperty("dropwizard.config")});
    }

    @Override
    public void initialize(Bootstrap<JModernConfiguration> bootstrap) {
    }

    @Override
    public void fiberRun(JModernConfiguration cfg, Environment env) throws ClassNotFoundException {
        JmxReporter.forRegistry(env.metrics()).build().start(); // Manually add JMX reporting (Dropwizard regression)

        ObjectGraph objectGraph = ObjectGraph.create(new ModernModule(cfg));
        env.jersey().register(objectGraph.get(HelloWorldResource.class));

        Feign.Builder feignBuilder = Feign.builder()
                .contract(new JAXRSModule.JAXRSContract())
                .encoder(new JacksonEncoder())
                .decoder(new JacksonDecoder());
        env.jersey().register(new ConsumerResource(feignBuilder));

        final IDBI dbi = new FiberDBIFactory().build(env, cfg.getDataSourceFactory(), "db");
        env.jersey().register(new DBResource(dbi));
    }

    // YAML Configuration
    public static class JModernConfiguration extends io.dropwizard.Configuration {
        @JsonProperty private @NotEmpty String template;
        @JsonProperty private @NotEmpty String defaultName;
        @Valid @NotNull @JsonProperty private DataSourceFactory database = new DataSourceFactory();

        public DataSourceFactory getDataSourceFactory() { return database; }
        public String getTemplate()    { return template; }
        public String getDefaultName() { return defaultName; }
    }

    // The actual service
    @Path("/hello-world")
    @Produces(MediaType.APPLICATION_JSON)
    public static class HelloWorldResource {
        private final AtomicLong counter = new AtomicLong();
        @Inject @Named("template") String template;
        @Inject @Named("defaultName") String defaultName;

        HelloWorldResource() {}

        @Timed // monitor timing of this service with Metrics
        @GET
        @Suspendable
        public Saying sayHello(@QueryParam("name") Optional<String> name) throws InterruptedException {
            final String value = String.format(template, name.or(defaultName));
            try {
                Fiber.sleep(ThreadLocalRandom.current().nextInt(10, 500));
            } catch (Throwable t) {
                throw new AssertionError(t);
            }
            return new Saying(counter.incrementAndGet(), value);
        }
    }

    @Path("/consumer")
    @Produces(MediaType.TEXT_PLAIN)
    public static class ConsumerResource {
        private final HelloWorldAPI helloWorld;

        public ConsumerResource(Feign.Builder feignBuilder) {
            this.helloWorld = feignBuilder.target(HelloWorldAPI.class, "http://localhost:8080");
        }

        @Timed
        @GET
        @Suspendable
        public String consume() {
            Saying saying = helloWorld.hi("consumer");
            return String.format("The service is saying: %s (id: %d)",  saying.getContent(), saying.getId());
        }
    }

    @Path("/db")
    @Produces(MediaType.APPLICATION_JSON)
    public static class DBResource {
        private final ModernDAO dao;

        public DBResource(IDBI idbi) {
            this.dao = idbi.onDemand(ModernDAO.class);

            try (Handle h = idbi.open()) {
                h.execute("create table something (id int primary key auto_increment, name varchar(100))");
                String[] names = { "Gigantic", "Bone Machine", "Hey", "Cactus" };
                Arrays.stream(names).forEach(name -> h.insert("insert into something (name) values (?)", name));
            }
        }

        @Timed
        @POST @Path("/add")
        public Something add(String name) throws SuspendExecution {
            return find(dao.insert(name));
        }

        @Timed
        @GET @Path("/item/{id}")
        public Something find(@PathParam("id") Integer id) throws SuspendExecution {
            return dao.findById(id);
        }

        @Timed
        @GET @Path("/all")
        public List<Something> all(@PathParam("id") Integer id) throws SuspendExecution {
            return dao.all();
        }
    }

    @RegisterMapper(SomethingMapper.class)
    public interface ModernDAO {
        @SqlUpdate("insert into something (name) values (:name)")
        @GetGeneratedKeys
        int insert(@Bind("name") String name);

        @SqlQuery("select * from something where id = :id")
        Something findById(@Bind("id") int id);

        @SqlQuery("select * from something")
        List<Something> all();
    }

    public static class Something {
        @JsonProperty public final int id;
        @JsonProperty public final String name;

        public Something(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    public static class SomethingMapper implements ResultSetMapper<Something> {
        public Something map(int index, ResultSet r, StatementContext ctx) throws SQLException {
            return new Something(r.getInt("id"), r.getString("name"));
        }
    }

    public interface HelloWorldAPI {
        @GET @Path("/hello-world")
        Saying hi(@QueryParam("name") String name);

        @GET @Path("/hello-world")
        Saying hi();
    }

    // JSON (immutable!) payload
    public static class Saying {
        private long id;
        private @Length(max = 10) String content;

        public Saying(long id, String content) {
            this.id = id;
            this.content = content;
        }

        public Saying() {} // required for deserialization

        @JsonProperty public long getId() { return id; }
        @JsonProperty public String getContent() { return content; }
    }

    @Module(injects = HelloWorldResource.class)
    public static class ModernModule {
        private final JModernConfiguration cfg;

        public ModernModule(JModernConfiguration cfg) {
            this.cfg = cfg;
        }

        @Provides @Named("template") String provideTemplate() {
            return cfg.getTemplate();
        }

        @Provides
        @Named("defaultName") String provideDefaultName() {
            return cfg.getDefaultName();
        }
    }
}
