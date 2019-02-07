package org.iota.ict.ec;

import org.iota.ict.Ict;
import org.iota.ict.model.Bundle;
import org.iota.ict.model.Transaction;
import org.iota.ict.network.event.GossipEvent;
import org.iota.ict.network.event.GossipFilter;
import org.iota.ict.network.event.GossipListener;

import java.util.HashSet;
import java.util.Set;

public class EconomicCluster implements GossipListener {

    private final Ict ict;
    private final Set<TrustedEconomicActor> actors = new HashSet<>();
    private final ECGossipFilter filter = new ECGossipFilter();

    EconomicCluster(Ict ict) {
        this.ict = ict;
        ict.addGossipListener(this);
    }

    public void addActor(TrustedEconomicActor actor) {
        actors.add(actor);
        filter.watchAddress(actor.getAddress());
    }

    public double determineApprovalConfidence(Transaction transaction) {
        double productOfTrustInverses = 1;
        for(TrustedEconomicActor actor : actors) {
            if(actor.approvesTransaction(transaction))
                productOfTrustInverses *= (1-actor.getTrust());
        }
        return 1-productOfTrustInverses;
    }

    @Override
    public void onGossipEvent(GossipEvent event) {
        Transaction transaction = event.getTransaction();
        if(!filter.passes(transaction))
            return;

        Bundle possiblyMarker = new Bundle(transaction);
        possiblyMarker.tryToComplete(ict);
        if(!possiblyMarker.isComplete() || !possiblyMarker.isStructureValid())
            return; // TODO if not complete, try again later

        for(TrustedEconomicActor actor : actors) {
            if(actor.getAddress().equals(transaction.address()))
                actor.processMarker(possiblyMarker);
        }
    }

    private class ECGossipFilter extends GossipFilter {

        @Override
        public boolean passes(Transaction transaction) {
            return transaction.isBundleHead && super.passes(transaction);
        }
    }
}
