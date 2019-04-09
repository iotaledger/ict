package org.iota.ict.ixi;

import org.iota.ict.Ict;
import org.iota.ict.IctTestTemplate;
import org.iota.ict.eee.EffectListener;
import org.iota.ict.eee.Environment;
import org.iota.ict.eee.chain.ChainedEffectListener;
import org.iota.ict.model.transaction.Transaction;
import org.iota.ict.model.transaction.TransactionBuilder;
import org.iota.ict.utils.Constants;
import org.iota.ict.utils.Trytes;
import org.junit.Assert;
import org.junit.Test;

public class GossipPreprocessorChainListenerTest extends IctTestTemplate {

    @Test
    public void testGossipPreprocessorChainListenerIsReceivingCorrectNumberOfEffects() {

        Ict ict = createIct();

        //create listener here to easily write the assertion.
        ReceiveEffectCounter effectListener = new ReceiveEffectCounter();

        DummyModule module = new DummyModule(ict, effectListener);
        module.start();
        saveSleep(100);

        Assert.assertEquals("Was expecting to receive exactly one effect",1,effectListener.receiveCount);
    }
}

/**
 * register a listener (ReceiveEffectCounter) for GOSSIP_PREPROCESSOR_CHAIN
 * then submit a transaction
 */
class DummyModule extends IxiModule {

    Transaction transaction = createTransaction();

    public DummyModule(Ixi ixi, EffectListener effectListener) {
        super(ixi);
        ixi.addListener(effectListener);
    }

    @Override
    public void run() {
        ixi.submit(transaction);
    }

    private static Transaction createTransaction() {
        TransactionBuilder builder = new TransactionBuilder();
        builder.address = Trytes.randomSequenceOfLength(Transaction.Field.ADDRESS.tryteLength);
        return builder.build();
    }
}

/**
 * receiveCount contains the number of effects received by this listener (counting number of calls to onReceive)
 */
class ReceiveEffectCounter implements EffectListener<ChainedEffectListener.Output> {

    int receiveCount = 0;
    @Override
    public void onReceive(ChainedEffectListener.Output effect) {
        receiveCount++;
    }

    @Override
    public Environment getEnvironment() {
        return Constants.Environments.GOSSIP_PREPROCESSOR_CHAIN;
    }
}