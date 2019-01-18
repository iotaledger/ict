package org.iota.ict.ixi;

import org.iota.ict.network.event.GossipListener;
import org.iota.ict.utils.RestartableThread;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class IxiModule extends RestartableThread implements Runnable {

    protected Ixi ixi;
    private Set<GossipListener> listeners = Collections.newSetFromMap(new ConcurrentHashMap<GossipListener, Boolean>());

    public IxiModule(Ixi ixi) {
        super(null);
        this.ixi = ixi;
    }

    public void terminate() {
        for(GossipListener listener : listeners)
            ixi.removeGossipListener(listener);
    }

}