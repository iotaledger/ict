package org.iota.ict.network;

import org.iota.ict.Ict;
import org.iota.ict.model.tangle.Tangle;
import org.iota.ict.model.transaction.Transaction;
import org.iota.ict.model.transaction.TransactionBuilder;
import org.junit.Assert;
import org.junit.Test;

public class TransactionRequestTest extends GossipTest {

    @Test
    public void testRequestingOverNewTransaction() {
        Ict a = createIct();
        Ict b = createIct();

        connect(a, b);

        Transaction original = submitTransactionAndReturnTransaction(a);
        waitUntilCommunicationEnds(100);
        Assert.assertNotNull("did not receive transaction", b.findTransactionByHash(original.hash));

        // scenario: existing ict forgets transaction
        a.getTangle().deleteTransaction(original);
        Assert.assertNull("did not delete original transaction", a.findTransactionByHash(original.hash));
        requestTransaction(a, original.hash);
        Assert.assertNotNull("could not request transaction from neighbor", a.findTransactionByHash(original.hash));

        // scenario: new ict joins
        Ict c = createIct();
        connect(a, c);
        Assert.assertNull("did not delete original transaction", c.findTransactionByHash(original.hash));
        requestTransaction(c, original.hash);
        Assert.assertNotNull("could not request transaction from neighbor", c.findTransactionByHash(original.hash));
    }

    @Test
    public void testRequestingOverOldTransaction() {
        Ict a = createIct();
        Ict b = createIct();

        connect(a, b);

        String oldTransactionHash = submitTransactionAndReturnTransaction(a).hash;
        Transaction original = submitTransactionAndReturnTransaction(a);
        waitUntilCommunicationEnds(100);

        // === scenario: existing ict forgets transaction ====
        a.getTangle().deleteTransaction(original);
        Assert.assertNull("did not delete original transaction", a.findTransactionByHash(original.hash));
        requestTransactionByRebroadcast(a, original.hash, oldTransactionHash);
        Assert.assertNotNull("could not request transaction from neighbor", a.findTransactionByHash(original.hash));

        // === scenario: new ict joins ===
        Ict c = createIct();
        connect(a, c);

        // old (already known) transaction acting as carrier for c's request which can't use oldTransaction1 because c hasn't received it either
        String anotherOldTransactionHash = submitTransactionAndReturnTransaction(a).hash;
        waitUntilCommunicationEnds(100);

        Assert.assertNull("did not delete original transaction", c.findTransactionByHash(original.hash));
        requestTransactionByRebroadcast(c, original.hash, anotherOldTransactionHash);
        Assert.assertNotNull("could not request transaction from neighbor", c.findTransactionByHash(original.hash));
    }

    private void requestTransaction(Ict ict, String hash) {
        ict.request(hash);
        ict.submit(new TransactionBuilder().build()); // request carrier
        waitUntilCommunicationEnds(100);
    }

    private void requestTransactionByRebroadcast(Ict sender, String hash, String carrierHash) {
        sender.request(hash);
        Transaction carrier = sender.findTransactionByHash(carrierHash);
        Tangle.TransactionLog log = sender.getTangle().findTransactionLog(carrier);
        log.senders.removeAll(log.senders);
        sender.broadcast(carrier);
        waitUntilCommunicationEnds(100);
    }

    private static Transaction submitTransactionAndReturnTransaction(Ict sender) {
        Transaction transaction = new TransactionBuilder().build();
        sender.submit(transaction);
        return transaction;
    }
}