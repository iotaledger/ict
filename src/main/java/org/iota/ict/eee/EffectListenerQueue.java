package org.iota.ict.eee;

import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class EffectListenerQueue<T> implements EffectListener<T> {

    private final String environment;
    private final BlockingQueue<T> effectQueue = new LinkedBlockingQueue<>();

    public EffectListenerQueue(String environment) {
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
    public String getEnvironment() {
        return environment;
    }

}
