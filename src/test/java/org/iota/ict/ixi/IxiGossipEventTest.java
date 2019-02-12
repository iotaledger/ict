package org.iota.ict.ixi;

import org.iota.ict.Ict;
import org.iota.ict.IctTestTemplate;
import org.iota.ict.model.transaction.Transaction;
import org.iota.ict.model.transaction.TransactionBuilder;
import org.iota.ict.network.gossip.GossipEvent;
import org.iota.ict.network.gossip.GossipListener;
import org.iota.ict.utils.Trytes;
import org.junit.*;

import java.util.Set;

public class IxiGossipEventTest extends IctTestTemplate {

    @Test
    public void testIxiReceivesGossipEvent() {

        // given
        Ict ict = createIct();
        TestIxiModule module = new TestIxiModule(ict);
        module.start();
        saveSleep(100);

        // then
        Assert.assertNotNull("IXI module did not receive gossip.", module.gossipEvent);
        Assert.assertEquals("Event received by IXI module contains wrong transaction", module.transaction, module.gossipEvent.getTransaction());
        Assert.assertNotNull("Ict did not store transaction submitted by IXI module.", ict.getTangle().findTransactionByHash(module.transaction.hash));
        Assert.assertTrue("IXI module can't query transaction from tangle.", module.findTransactionsByAddress(module.transaction.address()).contains(module.transaction));
    }
}

class TestIxiModule extends IxiModule {

    Transaction transaction = createTransaction();
    GossipEvent gossipEvent = null;

    public TestIxiModule(Ixi ixi) {
        super(ixi);
        ixi.addGossipListener(new GossipListener() {
            @Override
            public void onGossipEvent(GossipEvent event) {
                gossipEvent = event;
            }
        });
    }

    @Override
    public void run() {
        ixi.submit(transaction);
    }

    Set<Transaction> findTransactionsByAddress(String address) {
        return ixi.findTransactionsByAddress(address);
    }

    private static Transaction createTransaction() {
        TransactionBuilder builder = new TransactionBuilder();
        builder.address = Trytes.randomSequenceOfLength(Transaction.Field.ADDRESS.tryteLength);
        return builder.build();
    }
}
