package org.iota.ict.network.event;

import org.iota.ict.Ict;
import org.iota.ict.utils.Properties;
import org.junit.Assert;
import org.junit.Test;

public class GossipEventDispatcherTest {

    private boolean eventReceived;

    @Test
    public void testListenersCanNotBlock() {
        Ict a = new Ict(new Properties());
        eventReceived = false;
        long start = System.currentTimeMillis();

        a.addGossipListener(new GossipListener() {
            @Override
            public void onTransactionSubmitted(GossipSubmitEvent e) {
                eventReceived = true;
                // try to block for a second
                sleep(5000);
            }
        });

        a.submit("Hello World");
        sleep(100);
        Assert.assertEquals("event not delivered to gossip listener", true, eventReceived);

        long duration = System.currentTimeMillis() - start;
        Assert.assertEquals("gossip listener blocked main thread", true, duration < 5000);

        a.terminate();
        sleep(100);
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}