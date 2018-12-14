package org.iota.ict.network.event;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class GossipEventDispatcher extends Thread {

    public final List<GossipListener> listeners = new LinkedList<>();
    private final ConcurrentLinkedQueue<GossipEvent> eventQueue = new ConcurrentLinkedQueue<>();
    private boolean running = true;

    @Override
    public void run() {
        while (running) {
            synchronized (eventQueue) {
                try {
                    eventQueue.wait(10000);
                } catch (InterruptedException e) {
                }
            }

            if (eventQueue.peek() != null) {
                GossipEvent event = eventQueue.poll();
                for (GossipListener listener : listeners)
                    listener.on(event);
            }
        }
    }

    public void terminate() {
        running = false;
        synchronized (eventQueue) {
            eventQueue.notify();
        }
    }

    public void notifyListeners(GossipEvent event) {
        eventQueue.add(event);
        synchronized (eventQueue) {
            eventQueue.notify();
        }
    }
}
