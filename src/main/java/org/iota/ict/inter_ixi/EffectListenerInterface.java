package org.iota.ict.inter_ixi;

public interface EffectListenerInterface<T> {

    void onReceive(T effect);

    T getEffect();

    String getEnvironmentName();

}
