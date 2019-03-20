package org.iota.ict.eee;

public interface EffectListener<T> {

    void onReceive(T effect);

    Environment getEnvironment();
}
