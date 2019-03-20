package org.iota.ict.eee;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SimpleEffectDispatcher<T> implements EffectDispatcher<T> {

    protected Map<String, Set<EffectListener<T>>> listenersByEnvironment = new HashMap<>();

    public void addListener(EffectListener listener) {
        Set<EffectListener<T>> effectListenersOfEnvironment = listenersByEnvironment.get(listener.getEnvironment());
        if(effectListenersOfEnvironment == null) {
            effectListenersOfEnvironment = new HashSet<>();
            listenersByEnvironment.put(listener.getEnvironment(), effectListenersOfEnvironment);
        }
        effectListenersOfEnvironment.add(listener);
    }

    public void submitEffect(String environment, T effect) {
        Set<EffectListener<T>> listeners = listenersByEnvironment.get(environment);
        if (listeners != null)
            for (EffectListener<T> listener : listeners)
                dispatch(listener, effect);
    }

    protected void dispatch(EffectListener<T> listener, T effect) {
        try {
            listener.onReceive(effect);
        } catch (Throwable t) {
            handleThrowableFromListener(t, listener);
        }
    }

    protected void handleThrowableFromListener(Throwable throwable, EffectListener<T> listener) {

    }
}
