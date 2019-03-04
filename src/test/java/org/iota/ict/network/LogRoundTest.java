package org.iota.ict.network;

import org.iota.ict.Ict;
import org.iota.ict.model.Transaction;
import org.iota.ict.utils.Stats;
import org.iota.ict.utils.Trytes;
import org.junit.Assert;
import org.junit.Test;

public class LogRoundTest extends GossipTest {

    @Test
    public void testTransactionCounting() {
        Ict a = createIct();
        Ict b = createIct();
        connect(a, b);

        a.request(Trytes.randomSequenceOfLength(81));
        sendMessages(a, 10);
        waitUntilCommunicationEnds(200);

        Stats statsForB = a.getNeighbors().get(0).getStats();
        Stats statsForA = b.getNeighbors().get(0).getStats();

        Assert.assertEquals("transaction was back-broadcasted (useless communication)", 0, statsForB.receivedAll);
        Assert.assertEquals("neighbor did not receive all transactions", 10, statsForA.receivedAll);
        Assert.assertEquals("neighbor considered new transaction as not new", 10, statsForA.receivedNew);
        Assert.assertEquals("neighbor received invalid transactions", 0, statsForA.invalid);
        Assert.assertEquals("neighbor did not add request to stats", 1, statsForA.requested);

        b.getNeighbors().get(0).newRound(10000, false);
        Stats statsForANew = b.getNeighbors().get(0).getStats();
        Assert.assertEquals("stats was not reset upon new round", 0, statsForANew.receivedAll);
        Assert.assertEquals("previous stats were lost upon new round", 10, statsForA.receivedAll);
    }
}
