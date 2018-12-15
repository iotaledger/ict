package org.iota.ict.ixi;

import org.iota.ict.Ict;
import org.iota.ict.utils.Properties;
import org.iota.ict.network.event.GossipReceiveEvent;
import org.iota.ict.network.event.GossipSubmitEvent;
import org.junit.Assert;
import org.junit.Test;

public class IxiTest {

    @Test
    public void testIxI() {
        TestIxi ixi = new TestIxi();

        Properties properties = new Properties();
        properties.ixis.add(TestIxi.NAME);
        properties.ixiEnabled = true;
        Ict ict = new Ict(properties);

        Assert.assertTrue("ict could not connect to ixi", ixi.connected);

        String message = "Hello World";
        String hash = ixi.submit(message).hash;
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
        }
        Assert.assertNotNull("ixi did not receive event", ixi.receivedGossipSubmitEvent);
        Assert.assertEquals("ixi did not receive correct information", message, ixi.receivedGossipSubmitEvent.getTransaction().decodedSignatureFragments);
        Assert.assertNotNull("ict did not store transaction submitted by ixi", ict.getTangle().findTransactionByHash(hash));

        ict.terminate();
    }
}

class TestIxi extends IxiModule {

    static final String NAME = "simple.ixi";

    boolean connected = false;
    GossipSubmitEvent receivedGossipSubmitEvent = null;

    TestIxi() {
        super(NAME);
    }

    @Override
    public void onTransactionReceived(GossipReceiveEvent event) {

    }

    @Override
    public void onIctConnect(String name) {
        connected = true;
    }

    @Override
    public void onTransactionSubmitted(GossipSubmitEvent event) {
        receivedGossipSubmitEvent = event;
    }
}
