package org.iota.ict.eee;

public class ChainedEffectListenerImplementation<T> extends EffectListenerQueue<T> implements ChainedEffectListener<T> {

    private final long chainPosition;
    private final String chainedEnvironment;
    private EffectDispatcher dispatcher;

    public ChainedEffectListenerImplementation(EffectDispatcher dispatcher, String chainedEnvironment, int chainPosition) {
        super( chainedEnvironment + "#" + chainPosition);
        this.chainedEnvironment = chainedEnvironment;
        this.dispatcher = dispatcher;
        this.chainPosition = chainPosition;
    }

    public long getChainPosition() {
        return chainPosition;
    }

    public void passOn(T effect) {
        dispatcher.submitEffect(chainedEnvironment, new Output<>(chainPosition, effect));
    }

    @Override
    public String getChainedEnvironment() {
        return chainedEnvironment;
    }

    public static final class Output<T> implements ChainedEffectListener.Output<T> {

        private final T effect;
        private final long chainPosition;

        @Override
        public T getEffect() {
            return effect;
        }

        @Override
        public long getChainPosition() {
            return chainPosition;
        }

        public Output(long chainPosition, T effect) {
            this.effect = effect;
            this.chainPosition = chainPosition;
        }
    }
}
