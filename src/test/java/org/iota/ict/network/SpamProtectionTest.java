package org.iota.ict.network;

import org.iota.ict.Ict;
import org.iota.ict.model.Transaction;
import org.iota.ict.utils.Properties;
import org.junit.Assert;
import org.junit.Test;

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
        assertTransactionDoesNotMakeItThrough(a,b);
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

        int[] prevReceivedAll = {3, 8, 12};
        int median = prevReceivedAll[1];
        int tolerance = median * (int) a.getProperties().maxTransactionsRelative;

        for(int i = 0; i < 3; i++)
           a.getNeighbors().get(i).stats.receivedAll = prevReceivedAll[i];
        a.newRound();

        a.getNeighbors().get(2).stats.receivedAll = tolerance - 10;
        testUnidirectionalCommunication(d, a, 10);
        assertTransactionDoesNotMakeItThrough(d, a);
    }

    private void assertTransactionDoesNotMakeItThrough(Ict sender, Ict receiver) {
        Transaction toIgnore = sender.submit("");
        waitUntilCommunicationEnds(100);
        Assert.assertNull("Spam protection failed: transaction passed.", receiver.getTangle().findTransactionByHash(toIgnore.hash));
    }
}