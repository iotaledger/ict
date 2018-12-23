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
            if(!address.getAddress().equals(InetAddress.getByName(address.getHostName())))
                address = new InetSocketAddress(address.getHostName(), address.getPort());
        } catch (UnknownHostException e) {
            logger.warn(e);
        }
    }

    public static void logHeader() {
        StringBuilder report = new StringBuilder();
        report.append(pad("ALL")).append('|');
        report.append(pad("NEW")).append('|');
        report.append(pad("REQ")).append('|');
        report.append(pad("INV"));

        report.append("   ").append("ADDRESS");
        logger.info(report);
    }


    public void newRound() {
        StringBuilder report = new StringBuilder();
        report.append(pad(stats.receivedAll)).append('|');
        report.append(pad(stats.receivedNew)).append('|');
        report.append(pad(stats.requested)).append('|');
        report.append(pad(stats.receivedInvalid));

        report.append("   ").append(address);
        logger.info(report);

        stats.newRound();
    }

    private static String pad(int value) {
        return pad(value + "");
    }

    private static String pad(String str) {
        return String.format("%1$-5s", str);
    }

    public InetSocketAddress getAddress() {
        return address;
    }
}
