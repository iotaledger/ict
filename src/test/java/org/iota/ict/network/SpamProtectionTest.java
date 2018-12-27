package org.iota.ict.network;

import org.iota.ict.Ict;
import org.junit.Test;

public class SpamProtectionTest extends GossipTest {

    @Test(expected = AssertionError.class)
    public void testMaxTransactionsPerRound() {

        Ict a = createIct();

        Ict b = createIct();
        Ict c = createIct();
        Ict d = createIct();

        connect(a, b);
        connect(a, c);
        connect(a, d);

        a.getNeighbors().get(0).stats.receivedAll = 7;
        a.getNeighbors().get(1).stats.receivedAll = 5000;
        a.getNeighbors().get(2).stats.receivedAll = 3;

        testUnidirectionalCommunication(c,a,1);

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
