package org.iota.ict.network.event;

public class GossipListener {
    void on(GossipEvent e) {
        if (e instanceof GossipReceiveEvent)
            onTransactionReceived((GossipReceiveEvent) e);
        else if (e instanceof GossipSubmitEvent)
            onTransactionSubmitted((GossipSubmitEvent) e);
        else
            throw new IllegalArgumentException("Unknown event: " + e.getClass().getName());
    }

    public void onTransactionReceived(GossipReceiveEvent e) {
    }

    public void onTransactionSubmitted(GossipSubmitEvent e) {
    }
}
