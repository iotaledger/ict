package org.iota.ict.network;

import org.iota.ict.Ict;
import org.iota.ict.model.Tangle;
import org.iota.ict.model.Transaction;
import org.junit.Assert;
import org.junit.Test;

public class TransactionRequestTest extends GossipTest {

    @Test
    public void testRequestingOverNewTransaction() {
        Ict a = createIct();
        Ict b = createIct();

        connect(a, b);

        Transaction original = a.submit("Hello World");
        waitUntilCommunicationEnds(100);
        Assert.assertNotNull("did not receive transaction", b.getTangle().findTransactionByHash(original.hash));

        // scenario: existing ict forgets transaction
        a.getTangle().deleteTransaction(original);
        Assert.assertNull("delete original transaction", a.getTangle().findTransactionByHash(original.hash));
        requestTransaction(a, original.hash);
        Assert.assertNotNull("request transaction from neighbor", a.getTangle().findTransactionByHash(original.hash));

        // scenario: new ict joins
        Ict c = createIct();
        connect(a, c);
        Assert.assertNull("did not delete original transaction", c.getTangle().findTransactionByHash(original.hash));
        requestTransaction(c, original.hash);
        Assert.assertNotNull("could not request transaction from neighbor", c.getTangle().findTransactionByHash(original.hash));
    }

    @Test
    public void testRequestingOverOldTransaction() {
        Ict a = createIct();
        Ict b = createIct();

        connect(a, b);

        Transaction oldTransaction1 = a.submit("old (already known) transaction acting as carrier");
        Transaction original = a.submit("Hello World");
        waitUntilCommunicationEnds(100);

        // === scenario: existing ict forgets transaction ====
        a.getTangle().deleteTransaction(original);
        Assert.assertNull("did not delete original transaction", a.getTangle().findTransactionByHash(original.hash));
        requestTransactionByRebroadcast(a, original.hash, oldTransaction1.hash);
        Assert.assertNotNull("could not request transaction from neighbor", a.getTangle().findTransactionByHash(original.hash));

        // === scenario: new ict joins ===
        Ict c = createIct();
        connect(a, c);

        // carrier for c's request who can't use oldTransaction1 because c hasn't received it either
        Transaction anotherOldTransaction2 = a.submit("old (already known) transaction acting as carrier");
        waitUntilCommunicationEnds(100);

        Assert.assertNull("did not delete original transaction", c.getTangle().findTransactionByHash(original.hash));
        requestTransactionByRebroadcast(c, original.hash, anotherOldTransaction2.hash);
        Assert.assertNotNull("could not request transaction from neighbor", c.getTangle().findTransactionByHash(original.hash));
    }

    private void requestTransaction(Ict ict, String hash) {
        ict.request(hash);
        ict.submit("request carrier");
        waitUntilCommunicationEnds(100);
    }

    private void requestTransactionByRebroadcast(Ict sender, String hash, String carrierHash) {
        sender.request(hash);
        Transaction carrier = sender.getTangle().findTransactionByHash(carrierHash);
        Tangle.TransactionLog log = sender.getTangle().findTransactionLog(carrier);
        log.senders.removeAll(log.senders);
        sender.rebroadcast(carrier);
        waitUntilCommunicationEnds(100);
    }
}