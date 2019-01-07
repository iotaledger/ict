package org.iota.ict.network;

import org.iota.ict.Ict;
import org.iota.ict.model.TransactionBuilder;
import org.iota.ict.utils.Constants;
import org.junit.Assert;
import org.junit.Test;

public class TimestampTest extends GossipTest {

    @Test
    public void testTimestampDiffTolerance() {
        TransactionBuilder builder = new TransactionBuilder();
        builder.issuanceTimestamp = System.currentTimeMillis() - (long) (Constants.TIMESTAMP_DIFFERENCE_TOLERANCE_IN_MILLIS * 1.2);

        Ict a = createIct();
        Ict b = createIct();
        connect(a, b);

        a.submit(builder.build());
        waitUntilCommunicationEnds(100);
        Neighbor.Stats statsForA = b.getNeighbors().get(0).stats;
        Assert.assertEquals("Ict did forward transaction with timestamp out of tolerated time interval.", 0, statsForA.receivedAll);
        b.newRound();

        builder.issuanceTimestamp = System.currentTimeMillis() - (long) (Constants.TIMESTAMP_DIFFERENCE_TOLERANCE_IN_MILLIS * 0.8);
        a.submit(builder.build());
        waitUntilCommunicationEnds(100);
        Assert.assertEquals("Ict did not receive transaction.", 1, statsForA.receivedAll);
        Assert.assertEquals("Ict rejected transaction with timestamp in tolerated time interval.", 0, statsForA.receivedInvalid);
    }
}
