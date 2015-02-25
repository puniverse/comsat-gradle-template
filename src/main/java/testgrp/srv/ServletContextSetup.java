package testgrp.srv;

import java.util.Set;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

/**
 * Programmatic registration of regular JAX-RS (possible thanks to Servlet 3.0+); uses SPI scanning in `META-INF/services`, not classpath scanning!!!
 *
 * @author circlespainter
 */
public class ServletContextSetup implements ServletContainerInitializer {
    @Override
    public void onStartup(Set<Class<?>> set, ServletContext sc) throws ServletException {
        // Register fiber-blocking Jersey servlet
        javax.servlet.ServletRegistration.Dynamic fiber = sc.addServlet("servlet", org.glassfish.jersey.servlet.ServletContainer.class);

        // Add Jersey configuration class
        fiber.setInitParameter("javax.ws.rs.Application", "testgrp.srv.JerseyApplication");

        // Set packages to be scanned for resources
        fiber.setInitParameter("jersey.config.server.provider.packages", "testgrp.srv");

        // Don't lazy-load (fail-fast)
        fiber.setLoadOnStartup(1);

        // Support async (needed by fiber-blocking)
        fiber.setAsyncSupported(true);

        // Mapping
        fiber.addMapping("/thread-jaxrs/*");
    }
}
