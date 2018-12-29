package org.iota.ict.ixi;

import org.iota.ict.Ict;
import org.iota.ict.model.Transaction;
import org.iota.ict.model.TransactionBuilder;
import org.iota.ict.network.event.GossipEvent;
import org.iota.ict.network.event.GossipFilter;
import org.iota.ict.utils.Properties;
import org.iota.ict.network.event.GossipReceiveEvent;
import org.iota.ict.network.event.GossipSubmitEvent;
import org.iota.ict.utils.Trytes;
import org.junit.*;

import java.util.HashSet;

public class IxiTest {

    private static Ict ict;
    private static TestIxi ixi;

    @BeforeClass
    public static void setUp() {

        Properties properties = new Properties();
        properties.minForwardDelay = 0;
        properties.maxForwardDelay = 5;
        properties.ixiEnabled = true;
        ict = new Ict(properties);
        sleep(100);

        ixi = new TestIxi(properties.name);
    }

    @Test
    public void testOrder() {
        testIxiFilter();
        testNotTransmittingBranchOrTrunk();
        // other order fails TODO find out why
    }

    public void testIxiFilter() {

        String message = "Hello World";
        TransactionBuilder builder = new TransactionBuilder();
        builder.asciiMessage(message);
        builder.address = Trytes.randomSequenceOfLength(Transaction.Field.ADDRESS.tryteLength);
        ixi.setGossipFilter(new GossipFilter().watchAddress(builder.address));

        Transaction transaction = builder.build();
        ixi.submit(transaction);
        sleep(100);

        Assert.assertNotNull("Ixi did not receive event.", ixi.receivedGossipSubmitEvent);
        Assert.assertEquals("Ixi did not receive correct information.", message, ixi.receivedGossipSubmitEvent.getTransaction().decodedSignatureFragments);
        Assert.assertNotNull("Ict did not store transaction submitted by ixi.", ict.getTangle().findTransactionByHash(transaction.hash));
        Assert.assertEquals("Ict did not store transaction submitted by ixi.", ixi.findTransactionsByAddress(transaction.address).size(), 1);

        ixi.receivedGossipSubmitEvent = null;
        builder.address = Trytes.randomSequenceOfLength(Transaction.Field.ADDRESS.tryteLength);
        transaction = builder.build();
        ixi.submit(transaction);
        sleep(200);

        Assert.assertNull("Gossip filter let transaction from unwatched address pass.", ixi.receivedGossipSubmitEvent);
    }

    public void testNotTransmittingBranchOrTrunk() {

        ixi.setGossipFilter(new GossipFilter());

        TransactionBuilder builder = new TransactionBuilder();
        Transaction branch = builder.build();

        builder.branchHash = branch.hash;
        Transaction referencer = builder.build();

        ict.submit(branch);
        ict.submit(referencer);
        sleep(200);

        Transaction requestedReferencer = ixi.findTransactionByHash(referencer.hash);
        Transaction requestedBranch = ixi.findTransactionByHash(referencer.branchHash);

        Assert.assertNotNull("IXI module could not request referencing transaction.", requestedReferencer);
        Assert.assertNotNull("IXI module could not request referenced transaction.", requestedBranch);
        Assert.assertNull("Transaction sent to IXI module included object reference to branch", requestedReferencer.getBranch());
    }

    @AfterClass
    public static void tearDown() {
        if (ict != null)
            ict.terminate();
        // TODO terminate ixi (should be done by ict.terminate())
        sleep(50);
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
        }
    }
}

class TestIxi extends IxiModule {

    static final String NAME = "simple.ixi";
    GossipSubmitEvent receivedGossipSubmitEvent = null;

    TestIxi(String ictName) {
        super(NAME, ictName);
    }

    @Override
    public void onTransactionReceived(GossipReceiveEvent event) {

    }

    @Override
    public void onTransactionSubmitted(GossipSubmitEvent event) {
        receivedGossipSubmitEvent = event;
    }
}
