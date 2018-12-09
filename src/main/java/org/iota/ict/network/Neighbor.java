package org.iota.ict.network;

import java.net.*;

public class Neighbor {
    private final InetSocketAddress address;
    public final Stats stats = new Stats();

    public Neighbor(InetSocketAddress address) {
        this.address = address;
    }

    public class Stats {
        public int receivedAll, receivedNew, receivedInvalid;
        public int prevReceivedAll, prevReceivedNew, prevReceivedInvalid;

        public void newRound() {
            prevReceivedAll = receivedAll;
            prevReceivedNew = receivedNew;
            prevReceivedInvalid = receivedInvalid;
            receivedAll = 0;
            receivedNew = 0;
            receivedInvalid = 0;
        }
    }

    public InetSocketAddress getAddress() {
        return address;
    }
}
