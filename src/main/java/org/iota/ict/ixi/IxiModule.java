package org.iota.ict.ixi;

import org.iota.ict.network.event.GossipListener;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class IxiModule implements Runnable {

    protected Ixi ixi;
    private Set<GossipListener> listeners = Collections.newSetFromMap(new ConcurrentHashMap<GossipListener, Boolean>());

    IxiModule(Ixi ixi) {
        this.ixi = ixi;
    }

    public void terminate() {
        for(GossipListener listener : listeners)
            ixi.removeGossipListener(listener);
    }

}