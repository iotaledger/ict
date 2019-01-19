package org.iota.ict.network.event;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.iota.ict.utils.RestartableThread;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class GossipEventDispatcher extends RestartableThread {

    private static final Logger LOGGER = LogManager.getLogger("GsspEvDisp");
    public final List<GossipListener> listeners = new LinkedList<>();
    private final BlockingQueue<GossipEvent> eventQueue = new LinkedBlockingQueue<>();

    public GossipEventDispatcher() {
        super(LOGGER);
    }

    @Override
    public void run() {
        while (isRunning()) {
            try {
                GossipEvent event = eventQueue.take();
                for (GossipListener listener : listeners)
                    listener.onGossipEvent(event);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void log() {
        LOGGER.debug("gossip listeners: " + listeners.size() + " / event queue size: " + eventQueue.size());
        if(eventQueue.size() > 1000)
            LOGGER.warn("There is a backlog of " + eventQueue.size() + " events to be dispatched. This can cause memory and communication issues. Possible causes are (1) An IXI modules is taking too long to process events, (2) you are running too many IXI modules or (3) there are too many transactions in the network.");
    }

    @Override
    public void onTerminate() {
        runningThread.interrupt();
    }

    public void notifyListeners(GossipEvent event) {
        eventQueue.add(event);
    }
}
