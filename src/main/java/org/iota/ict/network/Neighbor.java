package org.iota.ict.network;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.*;

public class Neighbor {

    public static final Logger logger = LogManager.getLogger();
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

    public void printStats() {
        StringBuilder report = new StringBuilder();
        report.append(stats.receivedAll).append('/');
        report.append(stats.receivedNew).append('/');
        report.append(stats.receivedInvalid);

        report.append(" [");
        report.append(stats.prevReceivedAll).append('/');
        report.append(stats.prevReceivedNew).append('/');
        report.append(stats.prevReceivedInvalid).append(']');

        report.append("   ").append(address);

        logger.info(report);
   }

    public InetSocketAddress getAddress() {
        return address;
    }
}
