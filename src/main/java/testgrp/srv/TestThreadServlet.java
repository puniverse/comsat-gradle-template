package testgrp.srv;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = "/*", asyncSupported = true)
public class TestThreadServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Set response content type
        resp.setContentType("text/html");
        try (PrintWriter out = resp.getWriter()) {
            Thread.sleep(10);
            out.println("<h1>Hello world</h1>");
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}