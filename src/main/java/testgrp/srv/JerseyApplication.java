package testgrp.srv;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * Jersey's configuration class
 *
 * @author circlespainter
 */
public class JerseyApplication extends ResourceConfig {
    public JerseyApplication() {
        register(new DependencyBinder()); // Support Dependency Injection

        register(JacksonFeature.class);   // Support jackson
    }
}