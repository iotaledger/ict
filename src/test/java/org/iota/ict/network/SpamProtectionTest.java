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
        properties.antiSpamAbs = maxTransactionsPerRound;

        Ict a = createIct();
        Ict b = createIct(properties);

        connect(a, b);

        Neighbor.Stats statsForA = b.getNeighbors().get(0).stats;

        statsForA.receivedAll = maxTransactionsPerRound - 10;
        testUnidirectionalCommunication(a, b, 10);
        assertTransactionDoesNotMakeItThrough(a, b);
    }

    private void assertTransactionDoesNotMakeItThrough(Ict sender, Ict receiver) {
        Transaction toIgnore = sender.submit("");
        waitUntilCommunicationEnds(100);
        Assert.assertNull("Spam protection failed: transaction passed.", receiver.getTangle().findTransactionByHash(toIgnore.hash));
    }
}