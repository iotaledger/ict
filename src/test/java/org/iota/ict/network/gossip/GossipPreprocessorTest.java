package org.iota.ict.network.gossip;

import org.iota.ict.Ict;
import org.iota.ict.IctTestTemplate;
import org.iota.ict.model.transaction.TransactionBuilder;
import org.junit.Assert;
import org.junit.Test;

public class GossipPreprocessorTest extends IctTestTemplate {

    @Test
    public void testInsertion() throws InterruptedException {
        GossipPreprocessor preprocessor1 = new GossipPreprocessor(1);
        GossipPreprocessor preprocessor2 = new GossipPreprocessor(2);
        CustomGossipListener gossipListener = new CustomGossipListener();
        Ict ict = createIct();

        ict.addGossipListener(gossipListener);
        ict.addGossipPreprocessor(preprocessor1);
        ict.addGossipPreprocessor(preprocessor2);

        ict.submit(new TransactionBuilder().build());
        saveSleep(50);

        Assert.assertEquals(1, preprocessor1.incoming.size());
        Assert.assertEquals(0, preprocessor2.incoming.size());
        Assert.assertNull(gossipListener.lastEvent);

        preprocessor1.passOn(preprocessor1.incoming.take());
        saveSleep(50);

        Assert.assertEquals(0, preprocessor1.incoming.size());
        Assert.assertEquals(1, preprocessor2.incoming.size());
        Assert.assertNull(gossipListener.lastEvent);

        preprocessor2.passOn(preprocessor2.incoming.take());
        saveSleep(50);

        Assert.assertEquals(0, preprocessor1.incoming.size());
        Assert.assertEquals(0, preprocessor2.incoming.size());
        Assert.assertNotNull(gossipListener.lastEvent);
    }

    private class CustomGossipListener implements GossipListener {

        private GossipEvent lastEvent;

        @Override
        public void onGossipEvent(GossipEvent event) {
            lastEvent = event;
        }
    }
}