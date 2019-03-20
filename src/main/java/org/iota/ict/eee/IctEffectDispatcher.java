package org.iota.ict.eee;

import org.apache.logging.log4j.LogManager;
import org.iota.ict.network.gossip.GossipPreprocessor;
import org.iota.ict.utils.Constants;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class IctEffectDispatcher extends ThreadedEffectDispatcher {

    private final List<GossipPreprocessor> gossipPreprocessorsOrderedByPosition = new LinkedList<>();

    public IctEffectDispatcher() {
        super(LogManager.getLogger("IctEffDisp"));
        addListener(new GossipPreprocessorHandler());
    }

    @Override
    public void addListener(EffectListener listener) {
        super.addListener(listener);
        if(listener instanceof GossipPreprocessor) {
            gossipPreprocessorsOrderedByPosition.add((GossipPreprocessor)listener);
            sortGossipPreprocessors();
        }
    }

    private void sortGossipPreprocessors() {
        Collections.sort(gossipPreprocessorsOrderedByPosition, new Comparator<GossipPreprocessor>() {
            @Override
            public int compare(GossipPreprocessor o1, GossipPreprocessor o2) {
                return Long.compare(o1.position, o2.position);
            }
        });
    }

    @Override
    public void removeListener(EffectListener listener) {
        if(listener instanceof GossipPreprocessor) {
            gossipPreprocessorsOrderedByPosition.remove(listener);
        }
        super.removeListener(listener);
    }

    private GossipPreprocessor findGossipPreprocessorSuccessor(long position) {
        for(GossipPreprocessor preprocessor : gossipPreprocessorsOrderedByPosition)
            if(preprocessor.position > position)
                return preprocessor;
        return null;
    }

    private class GossipPreprocessorHandler implements EffectListener<GossipPreprocessor.Output> {
        @Override
        public void onReceive(GossipPreprocessor.Output output) {
            GossipPreprocessor successor = findGossipPreprocessorSuccessor(output.preprocessorPosition);
            if(successor != null) {
                submitEffect(Constants.Environments.GOSSIP_PREPROCESSOR_INPUT + "#"+successor.position, output.event);
            } else {
                submitEffect(Constants.Environments.GOSSIP, output.event);
            }
        }

        @Override
        public String getEnvironment() {
            return Constants.Environments.GOSSIP_PREPROCESSOR_OUTPUT;
        }
    }
}
