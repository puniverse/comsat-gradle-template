package testgrp.srv;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.servlet.FiberHttpServlet;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = "/*", asyncSupported = true)
public class TestFiberServlet extends FiberHttpServlet {
    public TestFiberServlet() {
        System.out.println("Init TestFiberServlet");
    }

    @Suspendable
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Set response content type
        resp.setContentType("text/html");
        try (PrintWriter out = resp.getWriter()) {
            Fiber.sleep(10);
            out.println("<h1>Hello world</h1>");
        } catch (InterruptedException | SuspendExecution ex) {
            throw new RuntimeException(ex);
        }
    }
}