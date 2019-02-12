package org.iota.ict.network;

import org.iota.ict.Ict;
import org.iota.ict.model.transaction.Transaction;
import org.iota.ict.model.transaction.TransactionBuilder;
import org.iota.ict.utils.Constants;
import org.iota.ict.utils.properties.EditableProperties;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

public class SpamProtectionTest extends GossipTest {

    @Test
    public void testMaxTransactionsPerRound() {

        int maxTransactionsPerRound = 666;

        EditableProperties properties = new EditableProperties();
        properties.antiSpamAbs(maxTransactionsPerRound);

        Ict a = createIct();
        Ict b = createIct(properties);

        connect(a, b);

        Neighbor.Stats statsForA = b.getNeighbors().get(0).stats;

        statsForA.receivedAll = maxTransactionsPerRound - 10;
        testUnidirectionalCommunication(a, b, 10);
        assertTransactionDoesNotMakeItThrough(a, b);
    }

    private void assertTransactionDoesNotMakeItThrough(Ict sender, Ict receiver) {
        Transaction toIgnore = new TransactionBuilder().build();
        waitUntilCommunicationEnds(100);
        Assert.assertNull("Spam protection failed: transaction passed.", receiver.findTransactionByHash(toIgnore.hash));
    }


    @Test
    public void testMWM() {
        TransactionBuilder builder = new TransactionBuilder();
        Set<Transaction> transactionsWithoutMWM = new HashSet<>();
        for(int i = 0; i < 10; i++)
            transactionsWithoutMWM.add(builder.build());

        Ict a = createIct();
        Ict b = createIct();
        connect(a, b);

        Constants.TESTING = false;
        for(Transaction t : transactionsWithoutMWM)
            a.submit(t);
        waitUntilCommunicationEnds(200);
        Constants.TESTING = true;

        Neighbor.Stats statsForA = b.getNeighbors().get(0).stats;
        Assert.assertTrue("Ict accepted transactions not satisfying MWM.", statsForA.receivedInvalid > transactionsWithoutMWM.size() * 0.7);
    }
}