package org.iota.ict.inter_ixi;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class EffectQueueListener<T> implements EffectListener<T> {

    private String environmentName;
    private Queue<T> effectQueue = new ConcurrentLinkedQueue<>();

    public EffectQueueListener(String environmentName) {
        this.environmentName = environmentName;
    }

    public T getEffect() {
        return effectQueue.poll();
    }

    @Override
    public void onReceive(T effect) {
        effectQueue.add(effect);
    }

    @Override
    public String getEnvironmentName() {
        return environmentName;
    }

}
