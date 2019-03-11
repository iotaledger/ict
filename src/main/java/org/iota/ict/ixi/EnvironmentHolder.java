package org.iota.ict.ixi;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class EnvironmentHolder {

    private Map<String, Set<EffectListener>> subscribedEffectListeners = new HashMap<>();

    public void addEffectListner(EffectListener effectListener) {
        Set<EffectListener> effectListeners = subscribedEffectListeners.get(effectListener);
        if(effectListeners == null)
            effectListeners = new HashSet<>();
        effectListeners.add(effectListener);
    }

    public void add(String environmentName, String effectTryes) {
        for(EffectListener listener: subscribedEffectListeners.get(environmentName))
            listener.addEffect(effectTryes);
    }

}
