package testgrp.srv;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.codahale.metrics.*;
import static com.codahale.metrics.MetricRegistry.name;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

/**
 * @author circlespainter
 */
@Singleton
@Path("/")
public class Resource {
    @Inject
    PersistenceService ps;

    private final Timer timer;

    public Resource() {
        this.timer = MetricsConf.metrics.timer(name(this.getClass(), "DB", "call", "duration"));
    }

    @GET
    @Path("hello")
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Hello world!";
    }

    @GET
    @Path("checkRO")
    @Produces(MediaType.TEXT_PLAIN)
    public String checkRO() throws IOException, InterruptedException {
        final Timer.Context context = timer.time();
        final Boolean res = new Boolean(ps.checkRO());
        context.stop();
        return res.toString();
    }

    @POST
    @Path("data")
    @Consumes(MediaType.APPLICATION_JSON)
    public void store(Data data) throws IOException, InterruptedException {
        ps.store(data);
    }
}