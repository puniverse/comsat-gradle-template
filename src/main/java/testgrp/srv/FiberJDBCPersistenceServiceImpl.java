package testgrp.srv;

import co.paralleluniverse.fibers.SuspendExecution;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Fiber-blocking JDBC implementation of the {@link PersistenceService}
 *
 * @author circlespainter
 */
public class FiberJDBCPersistenceServiceImpl implements PersistenceService {

    private DataSource ds;

    private void init() {
        try {
            if (ds == null) {
                Context initCtx = new InitialContext();
                Context envCtx = (Context) initCtx.lookup("java:comp/env");
                this.ds = (DataSource) envCtx.lookup("jdbc/fiberds");
            }
        } catch (final NamingException ne) {
            throw new RuntimeException(ne);
        }
    }

    @Override
    public void store(Data pb) throws IOException, InterruptedException, SuspendExecution  {
        init();
        // TODO
    }

    @Override
    public boolean checkRO() throws IOException, InterruptedException, SuspendExecution {
        init();
        try (final Connection c = ds.getConnection()) {
            return c.getMetaData().isReadOnly();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}