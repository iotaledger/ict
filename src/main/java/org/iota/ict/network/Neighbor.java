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

    public static final Logger logger = LogManager.getLogger("Neighbor");
    private String address;
    private InetSocketAddress socketAddress;
    public final Stats stats = new Stats();
    private double maxAllowedTransactionsForRound;

    public Neighbor(String address, long maxTransactionsAbsolute) {
        this.address = address;
        String host = address.split(":")[0];
        int port = Integer.parseInt(address.split(":")[1]);
        this.socketAddress = new InetSocketAddress(host, port);
        this.maxAllowedTransactionsForRound = maxTransactionsAbsolute;
    }

    public class Stats {
        public long receivedAll, receivedNew, receivedInvalid, requested, ignored;
        public long prevReceivedAll, prevReceivedNew, prevReceivedInvalid, prevRequested, prevIgnored;

        public void newRound() {
            prevReceivedAll = receivedAll;
            prevReceivedNew = receivedNew;
            prevReceivedInvalid = receivedInvalid;
            prevRequested = requested;
            prevIgnored = ignored;
            receivedAll = 0;
            receivedNew = 0;
            receivedInvalid = 0;
            requested = 0;
            ignored = 0;
        }
    }

    public void resolveHost() {
        try {
            if (!socketAddress.getAddress().equals(InetAddress.getByName(socketAddress.getHostName())))
                socketAddress = new InetSocketAddress(socketAddress.getHostName(), socketAddress.getPort());
        } catch (UnknownHostException e) {
            logger.warn(("Unknown Host for: " + socketAddress.getHostString()) + " (" + e.getMessage() + ")");
        }
    }

    public boolean sentPacket(DatagramPacket packet) {
        //if (address.equals(packet.getSocketAddress()))
        //    return true;
        boolean sameIP = sentPacketFromSameIP(packet);
        boolean samePort = socketAddress.getPort() == packet.getPort();
        return sameIP && samePort;
    }

    public boolean sentPacketFromSameIP(DatagramPacket packet) {
        try {
            return socketAddress.getAddress().getHostAddress().equals(packet.getAddress().getHostAddress());
        } catch (NullPointerException e) {
            // cannot resolve ip
            return false;
        }
    }

    public void newRound(long maxAllowedTransactionsForRound) {
        this.maxAllowedTransactionsForRound = maxAllowedTransactionsForRound;
        reportStatsOfRound();
        stats.newRound();
    }

    private void reportStatsOfRound() {
        StringBuilder report = new StringBuilder();
        report.append(pad(stats.receivedAll)).append('|');
        report.append(pad(stats.receivedNew)).append('|');
        report.append(pad(stats.requested)).append('|');
        report.append(pad(stats.receivedInvalid)).append('|');
        report.append(pad(stats.ignored));
        report.append("   ").append(socketAddress);
        logger.info(report);
    }

    public boolean reachedLimitOfAllowedTransactions() {
        return stats.receivedAll >= maxAllowedTransactionsForRound;
    }

    private static String pad(long value) {
        return pad(value + "");
    }

    private static String pad(String str) {
        return String.format("%1$-5s", str);
    }

    public InetSocketAddress getSocketAddress() {
        return socketAddress;
    }

    public String getAddress() {
        return address;
    }
}
