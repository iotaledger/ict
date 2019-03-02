package org.iota.ict.network;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.iota.ict.utils.Constants;
import org.iota.ict.utils.Stats;

import java.net.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * This class defines a neighbored Ict node. Neighbor nodes usually run remotely on a different device and connection
 * is established via the Internet. Besides the address, this class collect stats about the transaction flow from the
 * neighbor.
 */
public class Neighbor {

    public static final Logger logger = LogManager.getLogger("Neighbor");
    private InetSocketAddress address;
    private List<Stats> statsHistory = new ArrayList<>();
    private double maxAllowedTransactionsForRound;

    public Neighbor(InetSocketAddress address, long maxTransactionsAbsolute) {
        this.address = address;
        this.maxAllowedTransactionsForRound = maxTransactionsAbsolute;
        statsHistory.add(new Stats(this));
    }

    public void resolveHost() {
        try {
            if (!address.getAddress().equals(InetAddress.getByName(address.getHostName())))
                address = new InetSocketAddress(address.getHostName(), address.getPort());
        } catch (UnknownHostException e) {
            logger.warn(("Unknown Host for: " + address.getHostString()) + " (" + e.getMessage() + ")");
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
        try {
            return address.getAddress().getHostAddress().equals(packet.getAddress().getHostAddress());
        } catch (NullPointerException e) {
            // cannot resolve ip
            return false;
        }
    }

    public void newRound(long maxAllowedTransactionsForRound, boolean log) {
        this.maxAllowedTransactionsForRound = maxAllowedTransactionsForRound;
        if(log) reportStatsOfRound();
        statsHistory.add(new Stats(this));
        while (statsHistory.size() > Constants.MAX_AMOUNT_OF_ROUNDS_STORED)
            statsHistory.remove(0);
    }

    private void reportStatsOfRound() {
        Stats stats = getStats();
        StringBuilder report = new StringBuilder();
        report.append(pad(stats.receivedAll)).append('|');
        report.append(pad(stats.receivedNew)).append('|');
        report.append(pad(stats.requested)).append('|');
        report.append(pad(stats.invalid)).append('|');
        report.append(pad(stats.ignored));
        report.append("   ").append(address);
        logger.info(report);
    }

    public Stats getStats() {
        return statsHistory.get(statsHistory.size()-1);
    }

    public List<Stats> getStatsHistory() {
        return new ArrayList<>(statsHistory);
    }

    public boolean reachedLimitOfAllowedTransactions() {
        return getStats().receivedAll >= maxAllowedTransactionsForRound;
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
