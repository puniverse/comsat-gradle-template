package testgrp.srv;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;

import java.io.IOException;

/**
 * Fiber-blocking sleep + NOP implementation of the {@link PersistenceService}
 *
 * @author circlespainter
 */
public class FiberSleepPersistenceServiceImpl implements PersistenceService {

    @Override
    public void store(Data pb) throws IOException, InterruptedException, SuspendExecution  {
        Fiber.sleep(1000);
    }

    @Override
    public boolean checkRO() throws IOException, InterruptedException, SuspendExecution {
        Fiber.sleep(100);
        return false;
    }
}