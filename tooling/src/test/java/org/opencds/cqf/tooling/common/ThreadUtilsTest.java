package org.opencds.cqf.tooling.common;

import org.opencds.cqf.tooling.common.r4.CqfmSoftwareSystemHelper;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class ThreadUtilsTest {

    @Test
    public void testExecute() {
        ExecutorService executorService = Executors.newCachedThreadPool();
        AtomicInteger x = new AtomicInteger();
        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            tasks.add(() -> {
                x.set(x.get() + 1);
                return null;
            });
        }
        ThreadUtils.executeTasks(tasks, executorService);
        assertTrue(executorService.isShutdown());
        int finalX = x.get();
        assertEquals(finalX, 5);
    }

    @Test
    public void testShutdown() {
        ExecutorService executorService = Executors.newCachedThreadPool();
        AtomicInteger x = new AtomicInteger();
        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 1; i <= 50; i++) {
            tasks.add(() -> {
                x.set(x.get() + 1);
                if (x.get() == 15) ThreadUtils.shutdownRunningExecutors();
                return null;
            });
        }
        ThreadUtils.executeTasks(tasks, executorService);
        assertTrue(executorService.isShutdown());
        int finalX = x.get();
        assertTrue(finalX < 45);
    }

    private int intX = 0;
    @Test
    public void testExecuteWithQueueList() {
        Queue<Callable<Void>> returnTasks = new ConcurrentLinkedQueue<>(testWithX());
        ThreadUtils.executeTasks(returnTasks );
        assertEquals(intX, 100);
    }

    private Queue<Callable<Void>> testWithX(){
        Queue<Callable<Void>> returnable = new ConcurrentLinkedQueue<>();
        intX = intX + 1;
        if (intX < 100){
            returnable.addAll(testWithX());
        }
        return returnable;
    }

}
