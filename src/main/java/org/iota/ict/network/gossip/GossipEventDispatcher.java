package org.iota.ict.network.gossip;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.iota.ict.utils.RestartableThread;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

public class GossipEventDispatcher extends RestartableThread {

    private static final Logger LOGGER = LogManager.getLogger("GsspEvDisp");
    public final List<GossipListener> listeners = new LinkedList<>();
    protected final List<GossipPreprocessor> gossipPreprocessors = new LinkedList<>();
    protected final BlockingQueue<GossipEvent> eventQueue;

    public GossipEventDispatcher() {
        super(LOGGER);
        GossipPreprocessor lastGossipPreprocessor = new GossipPreprocessor(Integer.MAX_VALUE);
        eventQueue = lastGossipPreprocessor.incoming;
        addGossipPreprocessor(lastGossipPreprocessor);
    }

    @Override
    public void run() {
        while (isRunning()) {

            try {
                GossipEvent event = eventQueue.take();
                for (GossipListener listener : listeners)
                    listener.onGossipEvent(event);
                event.getTransaction().compress(); // TODO compress only if nobody is using this transaction anymore
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void log() {
        LOGGER.debug("gossip listeners: " + listeners.size() + " / gossip queue size: " + eventQueue.size());
        if (eventQueue.size() > 1000)
            LOGGER.warn("There is a backlog of " + eventQueue.size() + " events to be dispatched. This can cause memory and communication issues. Possible causes are (1) An IXI modules is taking too long to process events, (2) you are running too many IXI modules or (3) there are too many transactions in the network.");
    }

    @Override
    public void onTerminate() {
        runningThread.interrupt();
    }

    public void notifyListeners(GossipEvent event) {
        gossipPreprocessors.get(0).incoming.add(event);
    }

    public void addGossipPreprocessor(GossipPreprocessor gossipPreprocessor) {
        gossipPreprocessors.add(gossipPreprocessor);
        Collections.sort(gossipPreprocessors, new Comparator<GossipPreprocessor>() {
            @Override
            public int compare(GossipPreprocessor o1, GossipPreprocessor o2) {
                return Integer.compare(o1.position, o2.position);
            }
        });
        linkGossipPreprocessors();
    }

    public void removeGossipPreprocessor(GossipPreprocessor gossipPreprocessor) {
        gossipPreprocessors.remove(gossipPreprocessor);
        linkGossipPreprocessors();
    }

    private void linkGossipPreprocessors() {
        for(int i = 0; i < gossipPreprocessors.size()-1; i++)
            gossipPreprocessors.get(i).setSuccessor(gossipPreprocessors.get(i+1));
    }
}
