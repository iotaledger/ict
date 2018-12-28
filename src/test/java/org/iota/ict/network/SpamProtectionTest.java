package org.iota.ict.network;

import org.iota.ict.Ict;
import org.iota.ict.model.Transaction;
import org.iota.ict.utils.Properties;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

public class SpamProtectionTest extends GossipTest {

    @Test
    public void testMaxTransactionsPerRound() {

        int maxTransactionsPerRound = 666;

        Properties properties = new Properties();
        properties.maxTransactionsPerRound = maxTransactionsPerRound;

        Ict a = createIct();
        Ict b = createIct(properties);

        connect(a, b);

        Neighbor.Stats statsForA = b.getNeighbors().get(0).stats;

        statsForA.receivedAll = maxTransactionsPerRound - 10;
        testUnidirectionalCommunication(a, b, 10);

    }

    @Test
    public void testNeighborRelativeSpamProtection() {

        Ict a = createIct();

        Ict b = createIct();
        Ict c = createIct();
        Ict d = createIct();

        connect(a, b);
        connect(a, c);
        connect(a, d);

        int all1 = 7, all2 = 3;
        int avg = (int)Math.floor((all1 + all2)/2.0);
        int tolerance = avg * 5;

        a.getNeighbors().get(0).stats.prevReceivedAll = all1;
        a.getNeighbors().get(1).stats.prevReceivedAll = all2;

        a.getNeighbors().get(2).stats.prevReceivedAll = tolerance;
        testUnidirectionalCommunication(d, a,1);

        a.getNeighbors().get(2).stats.prevReceivedAll = tolerance+1;
        assertTransactionDoesNotMakeItThrough(d, a);
    }

    private void assertTransactionDoesNotMakeItThrough(Ict sender, Ict receiver) {
        Transaction toIgnore = sender.submit("");
        waitUntilCommunicationEnds(100);
        Assert.assertNull("Spam protection failed: more transactions than max_transactions_per_round passed.", receiver.getTangle().findTransactionByHash(toIgnore.hash));
    }
}