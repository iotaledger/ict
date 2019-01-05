package org.iota.ict.ixi;

import org.iota.ict.Ict;
import org.iota.ict.IctTestTemplate;
import org.iota.ict.model.Transaction;
import org.iota.ict.model.TransactionBuilder;
import org.iota.ict.network.event.GossipEvent;
import org.iota.ict.network.event.GossipFilter;
import org.iota.ict.network.event.GossipListener;
import org.iota.ict.utils.Properties;
import org.iota.ict.utils.Trytes;
import org.junit.*;

public class IxiTest extends IctTestTemplate {

    @Test
    public void testIxi() {

        // given
        Ict ict = createIct();
        TestIxi ixi = new TestIxi(new IctProxy(ict));
        Transaction transaction = createTransaction();

        // when
        ixi.submit(transaction);
        safeSleep(100);

        // then
        Assert.assertNotNull("IXI module did not receive event.", ixi.gossipEvent);
        Assert.assertEquals("Event received by IXI module contains wrong transaction", transaction, ixi.gossipEvent.getTransaction());
        Assert.assertNotNull("Ict did not store transaction submitted by IXI module.", ict.getTangle().findTransactionByHash(transaction.hash));
        Assert.assertTrue("IXI module can't query transaction from tangle.", ixi.findTransactionsByAddress(transaction.address).contains(transaction));
    }

    private static void safeSleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
        }
    }

    private static Transaction createTransaction() {
        TransactionBuilder builder = new TransactionBuilder();
        builder.address = Trytes.randomSequenceOfLength(Transaction.Field.ADDRESS.tryteLength);
        return builder.build();
    }
}

class TestIxi extends IxiModule {

    GossipEvent gossipEvent = null;

    public TestIxi(IctProxy ict) {
        super(ict);
        ict.addGossipListener(new GossipListener() {
            @Override
            public void onGossipEvent(GossipEvent event) {
                gossipEvent = event;
            }
        });
    }

    @Override
    public void run() {

    }
}
