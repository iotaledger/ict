package org.iota.ict.eee;

import java.util.*;

public class EventDispatcher {

    private Map<String, Set<EffectListener>> listenersByEnvironment = new HashMap<>();

    public void addListener(EffectListener effectListener) {
        Set<EffectListener> effectListenersOfEnvironment = listenersByEnvironment.get(effectListener.getEnvironment());
        if(effectListenersOfEnvironment == null) {
            effectListenersOfEnvironment = new HashSet<>();
            listenersByEnvironment.put(effectListener.getEnvironment(), effectListenersOfEnvironment);
        }
        effectListenersOfEnvironment.add(effectListener);
    }

    public void submitEffect(String environment, Object effect) {
        Set<EffectListener> listeners = listenersByEnvironment.get(environment);
        if(listeners != null)
            for(EffectListener listener : listeners)
                listener.onReceive(effect);
    }

}
