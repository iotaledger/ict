package org.iota.ict.utils;

import org.junit.Assert;
import org.junit.Test;

public class RestartableThreadTest {

    @Test
    public void testInterfaceCalls() {
        Worker underTest = new Worker();

        underTest.assertCalls(false, false, false);
        for(int i = 0; i < 5; i++) {
            underTest.resetCalls();
            underTest.start();
            safeSleep(10); // allow thread to call run()
            underTest.assertCalls(true, true, false);

            underTest.resetCalls();
            underTest.terminate();
            underTest.assertCalls(false, false, true);
        }
    }

    @Test
    public void testSubWorkerInterfaceCalls() {
        Worker superWorker = new Worker();
        Worker subWorker = new Worker();
        Worker subSubWorker = new Worker();
        superWorker.subWorkers.add(subWorker);
        subWorker.subWorkers.add(subSubWorker);

        subSubWorker.assertCalls(false, false, false);
        for(int i = 0; i < 5; i++) {
            subSubWorker.resetCalls();
            superWorker.start();
            safeSleep(10); // allow thread to call run()
            subSubWorker.assertCalls(true, true, false);

            subSubWorker.resetCalls();
            superWorker.terminate();
            subSubWorker.assertCalls(false, false, true);
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testCallTerminateBeforeStarting() {
        Worker underTest = new Worker();
        underTest.terminate();
    }

    @Test(expected = IllegalStateException.class)
    public void testStartingWhileAlreadyRunning() {
        Worker underTest = new Worker();
        underTest.start();
        underTest.start();
    }

    private static void safeSleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}

class Worker extends RestartableThread {

    Worker() {
        super(null);
    }

    private boolean called_run = false;
    private boolean called_onStart = false;
    private boolean called_onTerminate = false;

    void resetCalls() {
        called_run = false;
        called_onStart = false;
        called_onTerminate = false;
    }

    void assertCalls(boolean expected_called_run, boolean expected_called_onStart, boolean expected_called_onTerminate) {
        Assert.assertEquals(expected_called_run, called_run);
        Assert.assertEquals(expected_called_onStart, called_onStart);
        Assert.assertEquals(expected_called_onTerminate, called_onTerminate);
    }

    @Override
    public void run() {
        called_run = true;
    }

    @Override
    public void onStart() {
        called_onStart = true;
    }

    @Override
    public void onTerminate() {
        called_onTerminate = true;
    }
}