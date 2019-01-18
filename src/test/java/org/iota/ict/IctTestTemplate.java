package org.iota.ict;

import org.iota.ict.network.Neighbor;
import org.iota.ict.utils.Properties;
import org.junit.After;

import java.util.HashSet;
import java.util.Set;

public abstract class IctTestTemplate {

    protected Set<Ict> runningIcts = new HashSet<>();
    private final static int DEFAULT_PORT = 1337;

    protected Ict createIct() {
        return createIct(new Properties());
    }

    protected Ict createIct(Properties properties) {
        properties.host("localhost").port(DEFAULT_PORT + runningIcts.size());
        properties.minForwardDelay = 0;
        properties.maxForwardDelay = 10;
        properties.guiEnabled = false;
        Ict ict = new Ict(properties);
        runningIcts.add(ict);
        return ict;
    }

    @After
    public void tearDown() {
        for (Ict ict : runningIcts)
            ict.terminate();
        runningIcts = new HashSet<>();
        sleep(50);
    }

    protected static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
        }
    }

    protected static void connect(Ict ict1, Ict ict2) {
        addNeighborToIct(ict1, ict2);
        addNeighborToIct(ict2, ict1);
    }

    private static void addNeighborToIct(Ict ict, Ict neighbor) {
        Properties properties = ict.getCopyOfProperties();
        properties.neighbors.add(neighbor.getAddress());
        ict.updateProperties(properties);
    }

    protected void waitUntilCommunicationEnds(long maxWaitTime) {
        int lastReceived;
        int newReceived = sumNeighborStatsReceivedTransaction(runningIcts);
        do {
            lastReceived = newReceived;
            sleep(50);
            maxWaitTime -= 50;
            newReceived = sumNeighborStatsReceivedTransaction(runningIcts);
        } while (lastReceived != newReceived && maxWaitTime > 0);
    }

    private static int sumNeighborStatsReceivedTransaction(Iterable<Ict> network) {
        int sum = 0;
        for (Ict ict : network)
            sum += sumNeighborStatsReceivedTransaction(ict);
        return sum;
    }

    private static int sumNeighborStatsReceivedTransaction(Ict ict) {
        int sum = 0;
        for (Neighbor nb : ict.getNeighbors())
            sum += nb.stats.receivedAll;
        return sum;
    }
}
