package org.iota.ict.eee;

public interface ChainedEffectListener<T> extends EffectListener<T> {

    long getChainPosition();

    void passOn(T effect);

    String getChainedEnvironment();

    interface Output<T> {
        T getEffect();
        long getChainPosition();
    }
}