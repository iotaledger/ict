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
        builder.issuanceTimestamp = System.currentTimeMillis() - Constants.TIMESTAMP_DIFFERENCE_TOLERANCE_IN_MILLIS;

        Ict a = createIct();
        Ict b = createIct();
        connect(a, b);

        a.submit(builder.build());
        waitUntilCommunicationEnds(100);
        Neighbor.Stats statsForA = b.getNeighbors().get(0).stats;
        Assert.assertEquals("Ict accepted transaction with timestamp out of tolerance interval.",1, statsForA.receivedInvalid);
        b.newRound();

        builder.issuanceTimestamp = System.currentTimeMillis() -(long)(Constants.TIMESTAMP_DIFFERENCE_TOLERANCE_IN_MILLIS * 0.9);
        a.submit(builder.build());
        waitUntilCommunicationEnds(100);
        Assert.assertEquals("Ict rejected transaction with timestamp in tolerance interval.",0, statsForA.receivedInvalid);
    }
}
