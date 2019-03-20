package org.iota.ict.network.gossip;

import org.iota.ict.eee.EffectListener;
import org.iota.ict.eee.Environment;
import org.iota.ict.utils.Constants;

public interface GossipListener extends EffectListener<GossipEvent> {

    abstract class Implementation implements GossipListener {
        @Override
        public Environment getEnvironment() {
            return Constants.Environments.GOSSIP;
        }
    }
}
