package org.opencds.cqf.tooling.common;

import org.opencds.cqf.tooling.common.r4.CqfmSoftwareSystemHelper;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class ThreadUtilsTest {

    private int x = 0;
    @BeforeTest
    public void setup(){
        x = 0;
    }
    @Test
    public void testExecute() throws InterruptedException {
        ExecutorService executorService = Executors.newCachedThreadPool();
        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            tasks.add(() -> {
                x++;
                return null;
            });
        }
        ThreadUtils.executeTasks(tasks, executorService);

        // Shut down the executor and wait for all tasks to complete
        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.SECONDS);

        assertTrue(executorService.isShutdown());

        assertEquals(x, 5);
    }

    @Test
    public void testShutdown() throws InterruptedException {
        ExecutorService executorService = Executors.newCachedThreadPool();
        List<Callable<Void>> tasks = new ArrayList<>();
        int max = 1000000;
        for (int i = 1; i <= max; i++) {
            tasks.add(() -> {
                x++;
                if (x == 15) ThreadUtils.shutdownRunningExecutors();
                return null;
            });
        }
        ThreadUtils.executeTasks(tasks, executorService);

        // Shut down the executor and wait for all tasks to complete
        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.SECONDS);

        assertTrue(x < max);

        assertTrue(executorService.isShutdown());
    }

    @Test
    public void testExecuteWithQueueList() {
        Queue<Callable<Void>> returnTasks = new ConcurrentLinkedQueue<>(testWithX());
        ThreadUtils.executeTasks(returnTasks);
        assertEquals(x, 100);
    }

    private Queue<Callable<Void>> testWithX(){
        Queue<Callable<Void>> returnable = new ConcurrentLinkedQueue<>();
        x++;
        if (x < 100){
            returnable.addAll(testWithX());
        }
        return returnable;
    }

}
