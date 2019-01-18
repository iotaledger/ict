package org.iota.ict.utils;

import org.apache.logging.log4j.Logger;

import java.util.LinkedList;
import java.util.List;

public abstract class RestartableThread implements Restartable, Runnable {

    protected Logger logger;
    protected State state = new StateTerminated();
    protected Thread runningThread;
    protected List<Restartable> subWorkers = new LinkedList<>();

    protected RestartableThread(Logger Logger) {
        this.logger = Logger;
    }

    public void onStart() {}
    public void onTerminate() {}
    public void onStarted() {}
    public void onTerminated() {}

    @Override
    public synchronized void start() {
        state.start();
    }

    @Override
    public synchronized void terminate() {
        state.terminate();
    }

    protected class State implements Restartable {

        protected final String name;

        protected State(String name) { this.name = name; }
        public void start() {  throwIllegalStateException("start"); }
        public void terminate() {  throwIllegalStateException("terminate");  }

        protected void throwIllegalStateException(String actionName) {
            throw new IllegalStateException("Action '" + actionName + "' cannot be performed from state '" + name + "'.");
        }
    }

    protected class StateTerminated extends State {
        protected StateTerminated() { super("terminated"); }

        @Override
        public void start() {
            if(logger != null)
                logger.debug("starting ...");
            state = new StateStarting();
            onStart();
            for(Restartable subWorker : subWorkers)
                subWorker.start();
            runningThread = new Thread(RestartableThread.this);
            state = new StateRunning();
            runningThread.start();
            onStarted();
            if(logger != null)
                logger.debug("started");
        }
    }

    protected class StateStarting extends State {
        protected StateStarting() { super("starting"); }
    }

    protected class StateRunning extends State {
        protected StateRunning() { super("running"); }

        @Override
        public void terminate() {
            if(logger != null)
                logger.debug("terminating ...");
            state = new StateTerminating();
            onTerminate();
            while (runningThread.isAlive())
                safeSleep(10);
            for(Restartable subWorker : subWorkers)
                subWorker.terminate();
            state = new StateTerminated();
            onTerminated();
            if(logger != null)
                logger.debug("terminated");
        }

        private void safeSleep(long ms) {
            try {
                Thread.sleep(ms);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    protected class StateTerminating extends State {
        protected StateTerminating() { super("terminate"); }
    }

    public boolean isRunning() {
        return state instanceof StateRunning;
    }
}
