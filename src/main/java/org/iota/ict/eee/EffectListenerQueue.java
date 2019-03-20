package org.iota.ict.eee;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class EffectListenerQueue<T> implements EffectListener<T> {

    private final Environment environment;
    private final BlockingQueue<T> effectQueue = new LinkedBlockingQueue<>();

    public EffectListenerQueue(Environment environment) {
        this.environment = environment;
    }

    public T pollEffect() {
        return effectQueue.poll();
    }

    public T takeEffect() throws InterruptedException {
        return effectQueue.take();
    }

    @Override
    public void onReceive(T effect) {
        effectQueue.add(effect);
    }

    @Override
    public Environment getEnvironment() {
        return environment;
    }

}
