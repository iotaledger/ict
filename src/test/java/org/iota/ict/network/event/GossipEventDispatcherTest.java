package org.iota.ict.network.event;

import org.iota.ict.Ict;
import org.iota.ict.IctTestTemplate;
import org.iota.ict.model.TransactionBuilder;
import org.iota.ict.utils.IssueCollector;
import org.junit.Assert;
import org.junit.Test;

import java.util.logging.ErrorManager;

public class GossipEventDispatcherTest extends IctTestTemplate {

    private boolean eventReceived;

    @Test
    public void testListenersCanNotBlock() {
        Ict ict = createIct();

        eventReceived = false;
        long start = System.currentTimeMillis();

        ict.addGossipListener(new GossipListener() {
            @Override
            public void onGossipEvent(GossipEvent e) {
                eventReceived = true;
                // try to block for a few second
                saveSleep(5000);
            }
        });

        ict.submit(new TransactionBuilder().build());
        saveSleep(100);
        Assert.assertEquals("event not delivered to gossip listener", true, eventReceived);

        long duration = System.currentTimeMillis() - start;
        Assert.assertEquals("gossip listener blocked main thread", true, duration < 2000);
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

        final GossipEventDispatcher eventDispatcher = new GossipEventDispatcher();

        eventDispatcher.listeners.add(new GossipListener() {
            @Override
            public void onGossipEvent(GossipEvent e) {
                throw new RuntimeException();
            }
        });
        eventDispatcher.start();
        eventDispatcher.notifyListeners(new GossipEvent(null, false));

        saveSleep(20);

        Thread.setDefaultUncaughtExceptionHandler(handlerBefore);

        Assert.assertEquals("There were uncatched exceptions", 0, IssueCollector.amountOfIndicidents());
    }
}