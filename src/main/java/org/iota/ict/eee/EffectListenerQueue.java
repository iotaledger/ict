package org.iota.ict.eee;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class EffectListenerQueue<T> implements EffectListener<T> {

    private final String environment;
    private final Queue<T> effectQueue = new ConcurrentLinkedQueue<>();

    public EffectListenerQueue(String environment) {
        this.environment = environment;
    }

    public T getEffect() {
        return effectQueue.poll();
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
