package org.iota.ict.inter_ixi;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class EnvironmentHolder {

    private Map<String, Set<EffectListener>> registeredEffectListeners = new HashMap<>();

    public void addEffectListner(EffectListener effectListener) {
        Set<EffectListener> effectListenersOfEnvironment = registeredEffectListeners.get(effectListener.getEnvironmentName());
        if(effectListenersOfEnvironment == null) {
            effectListenersOfEnvironment = new HashSet<>();
            registeredEffectListeners.put(effectListener.getEnvironmentName(), effectListenersOfEnvironment);
        }
        effectListenersOfEnvironment.add(effectListener);
    }

    public void submitEffect(String environmentName, String effectTryes) {
        for(EffectListener listener: registeredEffectListeners.get(environmentName))
            listener.onReceive(effectTryes);
    }

}
