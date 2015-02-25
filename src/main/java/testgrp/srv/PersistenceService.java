package testgrp.srv;

import java.io.IOException;

/**
 * @author circlespainter
 */
public interface PersistenceService {
    public void store(Data d) throws IOException, InterruptedException;
    public boolean checkRO() throws IOException, InterruptedException;
}
