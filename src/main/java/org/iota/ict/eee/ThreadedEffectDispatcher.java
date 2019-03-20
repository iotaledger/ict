package org.iota.ict.eee;

import org.apache.logging.log4j.Logger;
import org.iota.ict.utils.RestartableThread;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ThreadedEffectDispatcher<T> extends RestartableThread implements EffectDispatcher<T> {

    protected final Logger logger;
    protected final EffectDispatcherImplementation implementation = new EffectDispatcherImplementation();
    protected final BlockingQueue<DispatchItem> toDispatch = new LinkedBlockingQueue<>();

    public ThreadedEffectDispatcher(Logger logger) {
        super(logger);
        this.logger = logger;
    }

    @Override
    public void run() {
        while (isRunning()) {
            try {
                DispatchItem dispatchItem = toDispatch.take();
                implementation.submitEffect(dispatchItem.environment, dispatchItem.effect);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void onTerminate() {
        runningThread.interrupt();
    }

    @Override
    public void addListener(EffectListener<T> listener) {
        implementation.addListener(listener);
    }

    @Override
    public void removeListener(EffectListener<T> listener) {
        implementation.removeListener(listener);
    }

    @Override
    public void submitEffect(Environment environment, T effect) {
        toDispatch.add(new DispatchItem(environment, effect));
    }

    public void log() {
        int amountOfListeners = 0;
        for(Map.Entry<Environment, Set<EffectListener<T>>> entry : implementation.listenersByEnvironment.entrySet())
            amountOfListeners += entry.getValue().size();

        int undispatched = toDispatch.size();
        logger.debug("gossip listeners: " + amountOfListeners + " / gossip queue size: " + undispatched);
        if (undispatched > 1000)
            logger.warn("There is a backlog of " + undispatched + " effects to be dispatched. This can cause memory and communication issues. Possible causes are (1) A listener is taking too long to process effects, (2) there are too many listeners (3) there are too many effects.");
        // TODO self-analyze cause
    }

    private class EffectDispatcherImplementation extends SimpleEffectDispatcher<T> {

        @Override
        protected void dispatch(EffectListener<T> listener, T effect) {
            if(isRunning())
                super.dispatch(listener, effect);
        }
    }

    private class DispatchItem {
        private final Environment environment;
        private final T effect;

        DispatchItem(Environment environment, T effect) {
            this.environment = environment;
            this.effect = effect;
        }
    }
}
