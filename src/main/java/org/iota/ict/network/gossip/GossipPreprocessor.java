package org.iota.ict.network.gossip;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class GossipPreprocessor {

    public final int position;
    private GossipPreprocessor successor;
    public final BlockingQueue<GossipEvent> incoming = new LinkedBlockingQueue<>();

    public GossipPreprocessor(int position) {
        this.position = position;
    }

    public void setSuccessor(GossipPreprocessor successor) {
        this.successor = successor;
    }

    public void passOn(GossipEvent event) {
        successor.incoming.add(event);
    }
}
