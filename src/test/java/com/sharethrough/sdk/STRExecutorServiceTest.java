package com.sharethrough.sdk;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.concurrent.ExecutorService;

import static org.mockito.Mockito.verify;

public class STRExecutorServiceTest extends TestBase {

    private ExecutorService subject;
    @Mock
    private Runnable runnable;

    @Before
    public void setUp() throws Exception {
        STRExecutorService.setExecutorService(null);
        subject = STRExecutorService.getInstance();
    }

    @Test
    public void execute_whenAllThreadsCrash_canStillExecuteJobs() throws Exception {
        int numberOfThreads = 10;
        for (int i = 0; i < numberOfThreads; i++) {
            final int finalI = i;
            subject.execute(new Runnable() {
                @Override
                public void run() {
                    throw new RuntimeException("Killing the thread " + finalI);
                }
            });
        }

        // Let the executor service run it's tasks
        Thread.sleep(100);

        subject.execute(runnable);

        // Let the executor service run it's tasks
        Thread.sleep(100);

        verify(runnable).run();
    }
}