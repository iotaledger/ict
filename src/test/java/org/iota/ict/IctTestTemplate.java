package org.iota.ict;

import org.iota.ict.network.Neighbor;
import org.iota.ict.utils.properties.EditableProperties;
import org.junit.After;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class IctTestTemplate {

    protected Set<Ict> runningIcts = new HashSet<>();
    private final static int DEFAULT_PORT = 1337;

    protected Ict createIct() {
        return createIct(new EditableProperties());
    }

    protected Ict createIct(EditableProperties properties) {
        properties.host("localhost")
                .port(DEFAULT_PORT + runningIcts.size())
                .minForwardDelay(0)
                .maxForwardDelay(10)
                .guiEnabled(false);
        Ict ict = new Ict(properties.toFinal());
        runningIcts.add(ict);
        return ict;
    }

    @After
    public void tearDown() {
        for (Ict ict : runningIcts)
            ict.terminate();
        runningIcts = new HashSet<>();
    }

    protected static void saveSleep(long ms) {
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
        EditableProperties properties = ict.getProperties().toEditable();
        List<InetSocketAddress> neighbors = properties.neighbors();
        neighbors.add(neighbor.getAddress());
        properties.neighbors(neighbors);
        ict.updateProperties(properties.toFinal());
    }

    private boolean anyIctStillCommunicating() {
        for (Ict ict : runningIcts)
            if(ict.node.getSenderQueueSize() > 0)
                return true;
        return false;
    }

    protected void waitUntilCommunicationEnds(long maxWaitTime) {

        long waitingSince = System.currentTimeMillis();

        do {
            saveSleep(10);
        } while (anyIctStillCommunicating() && System.currentTimeMillis() - waitingSince < maxWaitTime);

        saveSleep(10);
    }
}
