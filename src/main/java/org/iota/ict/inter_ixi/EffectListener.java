package org.iota.ict.inter_ixi;

import java.util.concurrent.ConcurrentLinkedQueue;

public class EffectListener<T> implements EffectListenerInterface<T> {

    private String environmentName;
    private ConcurrentLinkedQueue<T> effectQueue = new ConcurrentLinkedQueue<>();

    public EffectListener(String environmentName) {
        this.environmentName = environmentName;
    }

    @Override
    public void onReceive(T effect) {
        effectQueue.add(effect);
    }

    @Override
    public T getEffect() {
        return effectQueue.poll();
    }

    @Override
    public String getEnvironmentName() {
        return environmentName;
    }

}
