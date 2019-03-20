package org.iota.ict.std;

import org.iota.ict.Ict;
import org.iota.ict.IctTestTemplate;
import org.iota.ict.eee.Environment;
import org.iota.ict.model.bundle.Bundle;
import org.iota.ict.model.bundle.BundleBuilder;
import org.iota.ict.model.transaction.Transaction;
import org.iota.ict.model.transaction.TransactionBuilder;
import org.iota.ict.network.gossip.GossipEvent;
import org.iota.ict.network.gossip.GossipListener;
import org.iota.ict.utils.Constants;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class BundleCollectorTest extends IctTestTemplate {

    @Test
    public void testCollectionOfCompleteBundle() {
        Ict ictA = createIct();
        Ict ictB = createIct();
        connect(ictA, ictB);
        CustomGossipListener customGossipListener = new CustomGossipListener();
        ictB.addListener(customGossipListener);
        List<Bundle> bundles = new LinkedList<>();

        int amountOfTests = 100;
        for(int i = 0; i < amountOfTests; i++) {
            Bundle bundle = createBundleOfRandomLength();
            bundles.add(bundle);
            submiTransactionsInRandomOrder(ictA, bundle.getTransactions());
            assertBundleComplete(ictA, bundle.getHead().hash);
        }
        waitUntilCommunicationEnds(500);

        for(Bundle bundle : bundles)
            assertBundleComplete(ictB, bundle.getHead().hash);
        Assert.assertEquals("Ict did not receive expected amount of bundle heads.", amountOfTests, customGossipListener.receivedHeadEvents.size());
    }

    @Test
    public void testIncompleteBundleWillNotPass() {
        Ict ictA = createIct();
        Ict ictB = createIct();
        connect(ictA, ictB);
        CustomGossipListener customGossipListener = new CustomGossipListener();
        ictB.addListener(customGossipListener);

        for(int amountOfTests = 100; amountOfTests > 0; amountOfTests--) {
            Bundle bundle = createBundleOfRandomLength();
            submitTransactionsPartially(ictA, bundle.getTransactions());
        }

        waitUntilCommunicationEnds(500);

        Assert.assertEquals("Ict received events from incomplete bundles.", 0, customGossipListener.receivedHeadEvents.size());
    }

    private static void assertBundleComplete(Ict ict, String hashOfBundleHead) {
        Transaction head = ict.findTransactionByHash(hashOfBundleHead);
        Assert.assertNotNull("Ict did not receive head of bundle", head);
        Bundle bundle = new Bundle(head);
        Assert.assertTrue("Ict did not receive complete bundle", bundle.isComplete());
    }

    private static void submitTransactionsPartially(Ict ict, List<Transaction> transactions) {
        transactions.remove((int)(Math.random() * transactions.size()));
        submiTransactionsInRandomOrder(ict, transactions);
    }

    private static void submiTransactionsInRandomOrder(Ict ict, List<Transaction> transactions) {
        Collections.shuffle(transactions);
        for(Transaction transaction : transactions) {
            ict.submit(transaction);
        }
    }

    private static Bundle createBundleOfRandomLength() {
        return createBundle((int)(Math.random() * 5)+1);
    }

    private static Bundle createBundle(int length) {
        BundleBuilder bundleBuilder = new BundleBuilder();
        while (length-- > 0)
            bundleBuilder.append(new TransactionBuilder());
        return bundleBuilder.build();
    }

    private static class CustomGossipListener extends GossipListener.Implementation {

        private List<GossipEvent> receivedHeadEvents = new LinkedList<>();

        @Override
        public void onReceive(GossipEvent event) {
            if(!event.isOwnTransaction() && event.getTransaction().isBundleHead)
                receivedHeadEvents.add(event);
        }

        @Override
        public Environment getEnvironment() {
            return Constants.Environments.GOSSIP;
        }
    }
}