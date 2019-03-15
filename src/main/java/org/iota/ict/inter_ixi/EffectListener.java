package org.iota.ict.inter_ixi;

public interface EffectListener<T> {

    void onReceive(T effect);

    String getEnvironmentName();

}
