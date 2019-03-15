package org.iota.ict.inter_ixi;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class EffectListenerHolder {

    private Map<String, Set<EffectListener>> registeredEffectListeners = new HashMap<>();

    public void addEffectListner(EffectListener effectListener) {
        Set<EffectListener> effectListenersOfEnvironment = registeredEffectListeners.get(effectListener.getEnvironmentName());
        if(effectListenersOfEnvironment == null) {
            effectListenersOfEnvironment = new HashSet<>();
            registeredEffectListeners.put(effectListener.getEnvironmentName(), effectListenersOfEnvironment);
        }
        effectListenersOfEnvironment.add(effectListener);
    }

    public void submitEffect(String environmentName, Object effect) {
        for(EffectListener listener: registeredEffectListeners.get(environmentName))
            listener.onReceive(effect);
    }

}
