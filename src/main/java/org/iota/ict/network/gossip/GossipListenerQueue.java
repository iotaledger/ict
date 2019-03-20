package org.iota.ict.network.gossip;

import org.iota.ict.eee.EffectListenerQueue;
import org.iota.ict.utils.Constants;

public class GossipListenerQueue extends EffectListenerQueue<GossipEvent> implements GossipListener {

    GossipListenerQueue() {
        super(Constants.Environments.GOSSIP);
    }
}