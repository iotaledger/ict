package org.iota.ict.network.event;

import org.iota.ict.Ict;
import org.iota.ict.IctTestTemplate;
import org.iota.ict.utils.Properties;
import org.junit.Assert;
import org.junit.Test;

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
                sleep(5000);
            }
        });

        ict.submit("Hello World");
        sleep(100);
        Assert.assertEquals("event not delivered to gossip listener", true, eventReceived);

        long duration = System.currentTimeMillis() - start;
        Assert.assertEquals("gossip listener blocked main thread", true, duration < 2000);
    }
}