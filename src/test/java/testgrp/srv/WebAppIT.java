package testgrp.srv;

import junit.framework.TestCase;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URL;
import java.net.HttpURLConnection;

public class WebAppIT extends TestCase {
    private String port;
    private String base;

    public void setUp() throws Exception {
        super.setUp();
        port = System.getProperty("servlet.port");
        base = System.getProperty("app.base");
    }

    public void testCallIndexPageJersey() throws Exception {
        Client client = ClientBuilder.newBuilder().build();
        Response res = client.target("http://localhost:" + port + "/" + base + "/thread-servlet-hello").request().get();
        assertEquals(200, res.getStatus());
        assertTrue(res.readEntity(String.class).contains("Hello world"));
    }

    public void testCallHelloJersey() throws Exception {
        Client client = ClientBuilder.newBuilder().build();
        Response res = client.target("http://localhost:" + port + "/" + base + "/thread-jaxrs/hello").request().get();
        assertEquals(200, res.getStatus());
        assertTrue(res.readEntity(String.class).contains("Hello world"));
    }

    public void testCallCheckJersey() throws Exception {
        Client client = ClientBuilder.newBuilder().build();
        Response res = client.target("http://localhost:" + port + "/" + base + "/thread-jaxrs/checkRO").request().get();
        assertEquals(200, res.getStatus());
        assertTrue(res.readEntity(String.class).contains("false"));
    }

    public void testCallIndexPageURLConnection() throws Exception {
        URL url = new URL("http://localhost:" + port + "/" + base + "/thread-servlet-hello");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.connect();
        assertEquals(200, connection.getResponseCode());
    }

    public void testCallJerseyPost() throws Exception {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost("http://localhost:" + port + "/" + base + "/thread-jaxrs/data");
        String payload = "{\"f1\" : \"v1\", \"f2\" : \"v2\"}";
        StringEntity se = new StringEntity(payload);
        se.setContentType("application/json");
        httpPost.setEntity(se);
        HttpResponse httpResponse = httpClient.execute(httpPost);
        assertEquals(204, httpResponse.getStatusLine().getStatusCode());
    }

    public void testCallJerseyPost2() throws Exception {
        Client client = ClientBuilder.newBuilder().build();
        String payload = "{\"f1\" : \"v1\", \"f2\" : \"v2\"}";
        Response res = client.target("http://localhost:" + port + "/" + base + "/thread-jaxrs/data").request().post(Entity.entity(payload, MediaType.APPLICATION_JSON_TYPE));
        assertEquals(204, res.getStatus());
    }
}