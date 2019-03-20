package org.iota.ict.network.gossip;

import org.iota.ict.Ict;
import org.iota.ict.IctTestTemplate;
import org.iota.ict.model.transaction.TransactionBuilder;
import org.junit.Assert;
import org.junit.Test;

public class GossipPreprocessorTest extends IctTestTemplate {

    @Test
    public void testInsertion() throws InterruptedException {
        Ict ict = createIct();

        GossipPreprocessor preprocessor1 = new GossipPreprocessor(ict,1);
        GossipPreprocessor preprocessor2 = new GossipPreprocessor(ict,2);
        CustomGossipListener gossipListener = new CustomGossipListener();

        ict.addListener(gossipListener);
        ict.addListener(preprocessor1);
        ict.addListener(preprocessor2);

        ict.submit(new TransactionBuilder().build());
        saveSleep(50);

        GossipEvent effect = preprocessor1.pollEffect();
        Assert.assertNotNull(effect);
        Assert.assertNull(preprocessor2.pollEffect());
        Assert.assertNull(gossipListener.lastEvent);

        preprocessor1.passOn(effect);
        saveSleep(50);

        Assert.assertNull(preprocessor1.pollEffect());
        effect = preprocessor2.pollEffect();
        Assert.assertNotNull(effect);
        Assert.assertNull(gossipListener.lastEvent);

        preprocessor2.passOn(effect);
        saveSleep(50);

        Assert.assertNull(preprocessor1.pollEffect());
        Assert.assertNull(preprocessor2.pollEffect());
        Assert.assertNotNull(gossipListener.lastEvent);
    }

    private class CustomGossipListener extends GossipListener.Implementation {

        private GossipEvent lastEvent;

        @Override
        public void onReceive(GossipEvent event) {
            lastEvent = event;
        }
    }
}