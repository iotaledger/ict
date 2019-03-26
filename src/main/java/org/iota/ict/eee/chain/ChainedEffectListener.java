package org.iota.ict.eee.chain;

import org.iota.ict.eee.EffectListener;

public interface ChainedEffectListener<T> extends EffectListener<T> {

    long getChainPosition();

    void passOn(T effect);

    ChainedEnvironment getChainedEnvironment();

    interface Output<T> {
        T getEffect();
        long getChainPosition();
    }
}