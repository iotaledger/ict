package org.iota.ict.network.gossip;

import org.iota.ict.eee.EffectListenerQueue;
import org.iota.ict.ixi.Ixi;
import org.iota.ict.utils.Constants;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class GossipPreprocessor extends EffectListenerQueue<GossipEvent> implements GossipListener {

    public final long position;
    private Ixi ixi;

    public GossipPreprocessor(Ixi ixi, int position) {
        super(Constants.Environments.GOSSIP_PREPROCESSOR_INPUT + "#" + position);
        this.ixi = ixi;
        this.position = position;
    }

    public void passOn(GossipEvent event) {
        ixi.submitEffect(Constants.Environments.GOSSIP_PREPROCESSOR_OUTPUT, new Output(position, event));
    }

    public static final class Output {
        public final GossipEvent event;
        public final long preprocessorPosition;

        public Output(long preprocessorPosition, GossipEvent event) {
            this.event = event;
            this.preprocessorPosition = preprocessorPosition;
        }
    }
}
