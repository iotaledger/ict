package org.iota.ict.network.gossip;

import org.iota.ict.eee.chain.ChainedEffectListenerImplementation;
import org.iota.ict.eee.dispatch.EffectDispatcher;
import org.iota.ict.utils.Constants;

public class GossipPreprocessor extends ChainedEffectListenerImplementation<GossipEvent> implements GossipListener {

    public GossipPreprocessor(EffectDispatcher dispatcher, int chainPosition) {
        super(dispatcher, Constants.Environments.GOSSIP_PREPROCESSOR_CHAIN, chainPosition);
    }
}
