package org.iota.ict.network.gossip;

import org.iota.ict.Ict;
import org.iota.ict.IctTestTemplate;
import org.iota.ict.eee.ThreadedEffectDispatcherWithChainSupport;
import org.iota.ict.model.transaction.Transaction;
import org.iota.ict.model.transaction.TransactionBuilder;
import org.iota.ict.utils.Constants;
import org.iota.ict.utils.IssueCollector;
import org.junit.Assert;
import org.junit.Test;

public class GossipEventDispatcherTest extends IctTestTemplate {

    private boolean eventReceived;

    @Test
    public void testListenersCanNotBlock() {
        Ict ict = createIct();

        eventReceived = false;
        long start = System.currentTimeMillis();

        ict.addListener(new GossipListener.Implementation() {
            @Override
            public void onReceive(GossipEvent e) {
                eventReceived = true;
                // try to block for a few second
                saveSleep(5000);
            }
        });

        ict.submit(new TransactionBuilder().build());
        saveSleep(100);
        Assert.assertTrue("gossip not delivered to gossip listener", eventReceived);

        long duration = System.currentTimeMillis() - start;
        Assert.assertTrue("gossip listener blocked main thread", duration < 2000);
    }

    @Test
    public void testCatchExceptionFromListener() {

        Thread.UncaughtExceptionHandler handlerBefore = Thread.getDefaultUncaughtExceptionHandler();

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                IssueCollector.collect(throwable);
                throwable.printStackTrace();
            }
        });

        final ThreadedEffectDispatcherWithChainSupport threadedEffectDispatcher = new ThreadedEffectDispatcherWithChainSupport();

        threadedEffectDispatcher.addListener(new GossipListener.Implementation() {
            @Override
            public void onReceive(GossipEvent e) {
                throw new RuntimeException();
            }
        });
        threadedEffectDispatcher.start();
        threadedEffectDispatcher.submitEffect(Constants.Environments.GOSSIP, new GossipEvent(Transaction.NULL_TRANSACTION, false));

        saveSleep(20);

        Thread.setDefaultUncaughtExceptionHandler(handlerBefore);

        IssueCollector.log();
        Assert.assertEquals("There were uncatched exceptions", 0, IssueCollector.amountOfIndicidents());
    }
}