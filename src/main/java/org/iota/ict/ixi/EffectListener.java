package org.iota.ict.ixi;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class EffectListener {

    private final String name;
    private final Queue<String> effectQueue = new ConcurrentLinkedQueue<>();

    public EffectListener(String name) {
        this.name = name;
    }

    public void addEffect(String trytes) {
        effectQueue.add(trytes);
    }

    public String getEffect() {
        return effectQueue.poll();
    }

    public String getName() {
        return name;
    }

}
