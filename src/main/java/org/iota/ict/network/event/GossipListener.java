package org.iota.ict.network.event;

public class GossipListener {
    void on(GossipEvent e) {
        if (e instanceof GossipReceiveEvent)
            onReceiveTransaction((GossipReceiveEvent) e);
        else if (e instanceof GossipSentEvent)
            onSentTransaction((GossipSentEvent) e);
        else
            throw new IllegalArgumentException("Unknown event: " + e.getClass().getName());
    }

    public void onReceiveTransaction(GossipReceiveEvent e) {
    }

    public void onSentTransaction(GossipSentEvent e) {
    }
}
