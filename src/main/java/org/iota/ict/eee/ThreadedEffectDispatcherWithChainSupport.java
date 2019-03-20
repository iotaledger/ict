package org.iota.ict.eee;

import org.apache.logging.log4j.LogManager;
import org.iota.ict.network.gossip.GossipPreprocessor;
import org.iota.ict.utils.Constants;

import java.util.*;

public class ThreadedEffectDispatcherWithChainSupport extends ThreadedEffectDispatcher {

    private final Set<String> chainedEnvironments = new HashSet<>();
    private final List<ChainedEffectListenerImplementation> chainedEffectListenersOrderedByPosition = new LinkedList<>();

    public ThreadedEffectDispatcherWithChainSupport() {
        super(LogManager.getLogger("TEDwCS"));
    }

    public void addChainedEnvironment(String chainedEnvironment, String finalEnvironment) {
        if(chainedEnvironments.contains(chainedEnvironment))
            throw new IllegalArgumentException("Chained environment " + chainedEnvironment + " is already registered.");
        addListener(new ChainedEnvironmentHandler(chainedEnvironment, finalEnvironment));
        chainedEnvironments.add(chainedEnvironment);
    }

    @Override
    public void addListener(EffectListener listener) {
        super.addListener(listener);
        if(listener instanceof ChainedEffectListener) {
            ChainedEffectListener chainedEffectListener = (ChainedEffectListener) listener;
            if(!chainedEnvironments.contains(chainedEffectListener.getChainedEnvironment()))
                throw new IllegalArgumentException("Chained environment " + chainedEffectListener.getChainedEnvironment() + " is not registered.");
            chainedEffectListenersOrderedByPosition.add((ChainedEffectListenerImplementation)listener);
            sortGossipPreprocessors();
        }
    }

    private void sortGossipPreprocessors() {
        Collections.sort(chainedEffectListenersOrderedByPosition, new Comparator<ChainedEffectListenerImplementation>() {
            @Override
            public int compare(ChainedEffectListenerImplementation o1, ChainedEffectListenerImplementation o2) {
                return Long.compare(o1.getChainPosition(), o2.getChainPosition());
            }
        });
    }

    @Override
    public void removeListener(EffectListener listener) {
        if(listener instanceof GossipPreprocessor) {
            chainedEffectListenersOrderedByPosition.remove(listener);
        }
        super.removeListener(listener);
    }

    private ChainedEffectListener findChainSuccessor(String environment, long position) {
        for(ChainedEffectListener chainedEffectListener : chainedEffectListenersOrderedByPosition)
            if(chainedEffectListener.getChainPosition() > position && chainedEffectListener.getChainedEnvironment().equals(environment))
                return chainedEffectListener;
        return null;
    }

    private class ChainedEnvironmentHandler implements EffectListener<ChainedEffectListener.Output> {

        private final String chainedEnvironment, finalEnvironment;

        private ChainedEnvironmentHandler(String chainedEnvironment, String finalEnvironment) {
            this.chainedEnvironment = chainedEnvironment;
            this.finalEnvironment = finalEnvironment;
        }

        @Override
        public void onReceive(ChainedEffectListener.Output output) {
            ChainedEffectListener successor = findChainSuccessor(chainedEnvironment, output.getChainPosition());
            if(successor != null) {
                submitEffect(chainedEnvironment + "#"+successor.getChainPosition(), output.getEffect());
            } else if(finalEnvironment != null) {
                submitEffect(finalEnvironment, output.getEffect());
            }
        }

        @Override
        public String getEnvironment() {
            return Constants.Environments.GOSSIP_PREPROCESSOR_CHAIN;
        }
    }
}
