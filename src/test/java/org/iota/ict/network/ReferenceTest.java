package org.iota.ict.network;

import org.iota.ict.Ict;
import org.iota.ict.model.transaction.Transaction;
import org.iota.ict.model.transaction.TransactionBuilder;
import org.junit.Assert;
import org.junit.Test;

public class ReferenceTest extends GossipTest {

    /**
     * The referenced transactions were broadcasted to all Icts before the referencing transaction was broadcasted.
     * Icts must immediately build the edges upon receiving the referencing transactio.
     */
    @Test
    public void testReferenceCreationOrdered() {
        Ict a = createIct();
        Ict b = createIct();
        Ict c = createIct();

        connect(a, b);
        connect(b, c);

        String branchHash = submitTransactionAndReturnHash(a);
        String trunkHash = submitTransactionAndReturnHash(b);
        waitUntilCommunicationEnds(200);

        String referrerHash = sendReferrer(c, branchHash, trunkHash);
        waitUntilCommunicationEnds(200);

        assertCorrectReferences(a, referrerHash);
        assertCorrectReferences(b, referrerHash);
        assertCorrectReferences(c, referrerHash);
    }

    /**
     * The receiving Ict is not aware of the referenced transactions when receiving the referencing transactions and must
     * request branch and trunk and build the edges upon receiving them.
     */
    @Test
    public void testReferenceOldTransactions() {
        Ict a = createIct();
        Ict b = createIct();

        String branchHash = submitTransactionAndReturnHash(a);
        String trunkHash = submitTransactionAndReturnHash(b);
        waitUntilCommunicationEnds(200);

        connect(a, b);

        // referrer is also used as request carrier
        String referrerHash = sendReferrer(b, branchHash, trunkHash);
        waitUntilCommunicationEnds(200);

        assertCorrectReferences(a, referrerHash);
        assertCorrectReferences(b, referrerHash);
    }

    /**
     * The referecing transaction is received before the referenced transactions are. Both sender and receiver must
     * build the edges upon sending/receiving them.
     */
    @Test
    public void testReceiveInverseOrder() {
        Ict a = createIct();
        Ict b = createIct();

        connect(a, b);
        Transaction branch = new TransactionBuilder().build();
        Transaction trunk = new TransactionBuilder().build();
        String referrerHash = sendReferrer(a, branch.hash, trunk.hash);
        waitUntilCommunicationEnds(200);

        a.submit(branch);
        a.submit(trunk);
        waitUntilCommunicationEnds(200);

        assertCorrectReferences(a, referrerHash);
        assertCorrectReferences(b, referrerHash);
    }

    /**
     * Icts must not request the full tangle history on every restart. Therefore an Ict should not request the branch
     * or trunk of transactions that were created before the Icts itself was started.
     */
    @Test
    public void testNotRequestOldTransactions() {
        Ict a = createIct();
        String hashOfReallyOld = submitTransactionAndReturnHash(a);
        String justABitTooOld = sendReferrer(a, hashOfReallyOld, hashOfReallyOld);
        saveSleep(100); // sleep just a bit so a does not accidentally send to b

        Ict b = createIct();
        connect(a, b);
        String newTransaction = sendReferrer(a, justABitTooOld, justABitTooOld);
        waitUntilCommunicationEnds(200);
        submitTransactionAndReturnHash(b); // carries the request
        waitUntilCommunicationEnds(200);

        assertCorrectReferences(b, newTransaction);
        Assert.assertNull("requested transaction which should be ignored because it was referenced by old transaction", b.findTransactionByHash(hashOfReallyOld));
    }

    private String sendReferrer(Ict sender, String branchHash, String trunkHash) {
        TransactionBuilder referrerBuilder = new TransactionBuilder();
        referrerBuilder.asciiMessage("referrer transaction");
        referrerBuilder.trunkHash = trunkHash;
        referrerBuilder.branchHash = branchHash;

        Transaction referrer = referrerBuilder.build();
        sender.submit(referrer);
        return referrer.hash;
    }

    private void assertCorrectReferences(Ict ict, String referrerHash) {
        Transaction referrer = ict.findTransactionByHash(referrerHash);
        Assert.assertNotNull("not received referrer " + referrerHash, referrer);
        Assert.assertNotNull("not received trunk " + referrer.trunkHash(), ict.findTransactionByHash(referrer.trunkHash()));
        Assert.assertNotNull("not received branch " + referrer.branchHash(), ict.findTransactionByHash(referrer.branchHash()));
        Assert.assertNotNull("not built edge to branch", referrer.getBranch());
        Assert.assertEquals("built edge to incorrect branch", referrer.branchHash(), referrer.getBranch().hash);
        Assert.assertNotNull("not built edge to trunk", referrer.getTrunk());
        Assert.assertEquals("built edge to incorrect trunk", referrer.trunkHash(), referrer.getTrunk().hash);
    }

    private static String submitTransactionAndReturnHash(Ict sender) {
        Transaction transaction = new TransactionBuilder().build();
        sender.submit(transaction);
        return transaction.hash;
    }
}
