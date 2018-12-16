package org.iota.ict.network;

import org.iota.ict.Ict;
import org.junit.Assert;
import org.junit.Test;

public class LogRoundTest extends GossipTest {

    @Test
    public void testTransactionCounting() {
        Ict a = createIct();
        Ict b = createIct();
        connect(a, b);

        sendMessages(a, 10);
        waitUntilCommunicationEnds(200);

        Neighbor.Stats statsForB = a.getNeighbors().get(0).stats;
        Neighbor.Stats statsForA = b.getNeighbors().get(0).stats;

        Assert.assertEquals("transaction was back-broadcasted (useless communication)", 0, statsForB.receivedAll);
        Assert.assertEquals("neighbor did not receive all transactions", 10, statsForA.receivedAll);
        Assert.assertEquals("neighbor considered new transaction as not new", 10, statsForA.receivedNew);
        Assert.assertEquals("neighbor received invalid transactions", 0, statsForA.receivedInvalid);

        statsForA.newRound();
        Assert.assertEquals("stats was not reset upon new round", 0, statsForA.receivedAll);
        Assert.assertEquals("previous stats were lost upon new round", 10, statsForA.prevReceivedAll);
    }
}
