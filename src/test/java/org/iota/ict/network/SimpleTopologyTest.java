package org.iota.ict.network;

import org.iota.ict.Ict;
import org.iota.ict.network.event.GossipListener;
import org.junit.Assert;
import org.junit.Test;

public class SimpleTopologyTest extends GossipTest {

    @Test
    public void testCommunication() {

        Ict a = createIct(1337);
        Ict b = createIct(1338);
        Ict c = createIct(1339);

        buildConnection(a, b);
        buildConnection(b, c);

        testBidirectionalCommunication(a, b, 20);
        testBidirectionalCommunication(b, c, 20);
        testBidirectionalCommunication(a, c, 30);
    }

    @Test
    public void testNoInfiniteLoopMessageForwarding() {

        Ict a = createIct(1337);
        Ict b = createIct(1338);
        Ict c = createIct(1339);

        buildConnection(a, b);
        buildConnection(a, c);
        buildConnection(b, c);

        c.addGossipListener(new GossipListener());

        int amountOfMessages = 10;
        for(int i = 0; i < amountOfMessages; i++)
            a.submit("Message #"+amountOfMessages);

        waitUntilCommunicationEnds(500);
        Assert.assertTrue("no infinite loop message forwarding", c.getNeighbors().get(0).stats.receivedAll <= amountOfMessages);
        Assert.assertTrue("no infinite loop message forwarding", c.getNeighbors().get(1).stats.receivedAll <= amountOfMessages);
        Assert.assertTrue("all messages received", c.getNeighbors().get(0).stats.receivedAll + c.getNeighbors().get(1).stats.receivedAll >= amountOfMessages);
    }
}
