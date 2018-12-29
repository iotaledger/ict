package org.iota.ict.network;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.iota.ict.Ict;
import org.iota.ict.utils.ErrorHandler;

import java.net.*;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * This class defines a neighbored Ict node. Neighbor nodes usually run remotely on a different device and connection
 * is established via the Internet. Besides the address, this class collect stats about the transaction flow from the
 * neighbor.
 */
public class Neighbor {

    public static final Logger logger = LogManager.getLogger(Neighbor.class);
    private InetSocketAddress address;
    public final Stats stats = new Stats();
    private long maxAllowedTransactionsForRound;

    public Neighbor(InetSocketAddress address, long maxTransactionsRelative) {
        this.address = address;
        this.maxAllowedTransactionsForRound = maxTransactionsRelative;
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
            if(!address.getAddress().equals(InetAddress.getByName(address.getHostName())))
                address = new InetSocketAddress(address.getHostName(), address.getPort());
        } catch (UnknownHostException e) {
            ErrorHandler.handleWarning(logger, e, "Unknown Host for: " + address.getHostString());
        }
    }

    public boolean sentPacket(DatagramPacket packet) {
        //if (address.equals(packet.getSocketAddress()))
        //    return true;
        boolean sameIP = sentPacketFromSameIP(packet);
        boolean samePort = address.getPort() == packet.getPort();
        return sameIP && samePort;
    }

    public boolean sentPacketFromSameIP(DatagramPacket packet) {
        return address.getAddress().getHostAddress().equals(packet.getAddress().getHostAddress());
    }

    public static void logHeader() {
        StringBuilder report = new StringBuilder();
        report.append(pad("ALL")).append('|');
        report.append(pad("NEW")).append('|');
        report.append(pad("REQ")).append('|');
        report.append(pad("INV")).append('|');
        report.append(pad("IGN"));

        report.append("   ").append("ADDRESS");
        logger.info(report);
    }

    public static void newRound(Ict ict, int round) {
        if(round % 10 == 0)
            Neighbor.logHeader();
        // two separate FOR-loops to prevent delays between newRound() calls
        for (Neighbor neighbor : ict.getNeighbors()) {
            long tolerance = calcTolerance(ict, neighbor);
            neighbor.newRound(tolerance);
        }
        for (Neighbor neighbor : ict.getNeighbors())
            neighbor.resolveHost();
    }

    private static long calcTolerance(Ict ict, Neighbor sender) {
        long relativeTolerance = calcReferenceForRelativeTolerance(ict.getNeighbors(), sender) * ict.getProperties().maxTransactionsRelative;
        return Math.min(ict.getProperties().maxTransactionsPerRound, relativeTolerance);
    }

    private static long calcReferenceForRelativeTolerance(List<Neighbor> allNeighbors, Neighbor sender) {
        List<Neighbor> otherNeighbors = new LinkedList<>(allNeighbors);
        otherNeighbors.remove(sender);
        return otherNeighbors.size() > 0 ? calcUpperMedianOfPrevReceivedAll(otherNeighbors) : 9999999;
    }

    private static long calcUpperMedianOfPrevReceivedAll(List<Neighbor> neighbors) {
        List<Long> values = new LinkedList<>();
        for(Neighbor nb : neighbors)
            values.add(nb.stats.prevReceivedAll);
        return calcUpperMedian(values);
    }

    private static long calcUpperMedian(List<Long> values) {
        assert values.size() > 0;
        Collections.sort(values);
        return values.get((int)Math.ceil((values.size()-1) / 2.0));
    }

    private void newRound(long maxAllowedTransactionsForRound) {
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
        report.append("   ").append(address);
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

    public InetSocketAddress getAddress() {
        return address;
    }
}
