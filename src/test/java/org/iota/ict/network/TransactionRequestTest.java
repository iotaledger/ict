package org.iota.ict.network;

import org.iota.ict.Ict;
import org.iota.ict.model.Transaction;
import org.junit.Assert;
import org.junit.Test;

public class TransactionRequestTest extends GossipTest {

    @Test
    public void testRequesting() {
        Ict a = createIct();
        Ict b = createIct();

        connect(a, b);

        Transaction original = a.submit("Hello World");
        waitUntilCommunicationEnds(100);

        // scenario: existing ict forgets transaction
        a.getTangle().deleteTransaction(original);
        Assert.assertNull("delete original transaction", a.getTangle().findTransactionByHash(original.hash));
        requestTransaction(a, original.hash);
        Assert.assertNotNull("request transaction from neighbor", a.getTangle().findTransactionByHash(original.hash));

        // scenario: new ict joins
        Ict c = createIct();
        connect(a, c);
        Assert.assertNull("delete original transaction", c.getTangle().findTransactionByHash(original.hash));
        requestTransaction(c, original.hash);
        Assert.assertNotNull("request transaction from neighbor", c.getTangle().findTransactionByHash(original.hash));
    }

    private void requestTransaction(Ict ict, String hash) {

        ict.request(hash);
        ict.submit("request carrier");
        waitUntilCommunicationEnds(100);
    }
}