package org.iota.ict.network;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.*;

/**
 * This class defines a neighbored Ict node. Neighbor nodes usually run remotely on a different device and connection
 * is established via the Internet. Besides the address, this class collect stats about the transaction flow from the
 * neighbor.
 */
public class Neighbor {

    public static final Logger logger = LogManager.getLogger();
    private InetSocketAddress address;
    public final Stats stats = new Stats();

    public Neighbor(InetSocketAddress address) {
        this.address = address;
    }

    public class Stats {
        public int receivedAll, receivedNew, receivedInvalid, requested;
        public int prevReceivedAll, prevReceivedNew, prevReceivedInvalid, prevRequested;

        public void newRound() {
            prevReceivedAll = receivedAll;
            prevReceivedNew = receivedNew;
            prevReceivedInvalid = receivedInvalid;
            prevRequested = requested;
            receivedAll = 0;
            receivedNew = 0;
            receivedInvalid = 0;
            requested = 0;
        }
    }

    public void resolveHost() {
        try {
            if(address.getAddress().equals(InetAddress.getByName(address.getHostName())))
                address = new InetSocketAddress(address.getHostName(), address.getPort());
        } catch (UnknownHostException e) {
            logger.warn(e);
        }
    }

    public void printStats() {
        StringBuilder report = new StringBuilder();
        report.append(stats.receivedAll).append('/');
        report.append(stats.receivedNew).append('/');
        report.append(stats.requested).append('/');
        report.append(stats.receivedInvalid);

        report.append(" [");
        report.append(stats.prevReceivedAll).append('/');
        report.append(stats.prevReceivedNew).append('/');
        report.append(stats.prevRequested).append('/');
        report.append(stats.prevReceivedInvalid).append(']');

        report.append("   ").append(address);

        logger.info(report);
    }

    public InetSocketAddress getAddress() {
        return address;
    }
}
