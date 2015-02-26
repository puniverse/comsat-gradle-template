package testgrp.srv;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;

import java.io.IOException;

/**
 * @author circlespainter
 */
public interface PersistenceService {
    public void store(Data d) throws IOException, InterruptedException, SuspendExecution;
    public boolean checkRO() throws IOException, InterruptedException, SuspendExecution;
}
