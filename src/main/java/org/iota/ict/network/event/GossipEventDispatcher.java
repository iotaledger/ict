package org.iota.ict.network.event;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class GossipEventDispatcher extends Thread {

    public final List<GossipListener> listeners = new LinkedList<>();
    private final BlockingQueue<GossipEvent> eventQueue = new LinkedBlockingQueue<>();

    @Override
    public void run() {
        while (!interrupted()) {
            try {
                GossipEvent event = eventQueue.take();
                for (GossipListener listener : listeners)
                    listener.onGossipEvent(event);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void terminate() {
        interrupt();
    }

    public void notifyListeners(GossipEvent event) {
        eventQueue.add(event);
    }

}
