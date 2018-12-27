package org.iota.ict.network;

import org.iota.ict.Ict;
import org.iota.ict.utils.Properties;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

public class SpamProtectionTest extends GossipTest {

    @Test
    public void testMaxTransactionsPerRoundJustEnough() {
        testMaxTransactionsPerRound(666, 666-3, 3);
    }

    @Test(expected = AssertionError.class)
    public void testMaxTransactionsPerRoundTooMany() {
        testMaxTransactionsPerRound(666, 666-3, 4);
    }

    public void testMaxTransactionsPerRound(int maxTransactionsPerRound, int alreadySent, int sendTransactions) {

        Properties properties = new Properties();
        properties.maxTransactionsPerRound = maxTransactionsPerRound;

        Ict a = createIct();
        Ict b = createIct(properties);

        connect(a, b);

        Neighbor.Stats statsForA = b.getNeighbors().get(0).stats;
        statsForA.receivedAll = alreadySent;

        testUnidirectionalCommunication(a, b, sendTransactions);
    }

    @Test(expected = Test.None.class /* no AssertionError expected */)
    public void testGoodTransactionsCanPass() {

        Ict a = createIct();

        Ict b = createIct();
        Ict c = createIct();
        Ict d = createIct();

        connect(a, b);
        connect(a, c);
        connect(a, d);

        a.getNeighbors().get(0).stats.prevReceivedAll = 7;
        a.getNeighbors().get(1).stats.prevReceivedAll = 25;
        a.getNeighbors().get(2).stats.prevReceivedAll = 3;

        testUnidirectionalCommunication(c,a,1);

    }

    @Test(expected = AssertionError.class)
    public void testBadTransactionsGetsFiltered() {

        Ict a = createIct();

        Ict b = createIct();
        Ict c = createIct();
        Ict d = createIct();

        connect(a, b);
        connect(a, c);
        connect(a, d);

        a.getNeighbors().get(0).stats.prevReceivedAll = 7;
        a.getNeighbors().get(1).stats.prevReceivedAll = 26;
        a.getNeighbors().get(2).stats.prevReceivedAll = 3;

        testUnidirectionalCommunication(c,a,1);

    }

}