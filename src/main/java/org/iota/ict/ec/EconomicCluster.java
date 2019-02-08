package org.iota.ict.ec;

import org.iota.ict.Ict;
import org.iota.ict.model.Bundle;
import org.iota.ict.model.Transaction;
import org.iota.ict.network.gossip.GossipEvent;
import org.iota.ict.network.gossip.GossipFilter;
import org.iota.ict.network.gossip.GossipListener;
import org.iota.ict.utils.properties.FinalProperties;
import org.iota.ict.utils.properties.PropertiesUser;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EconomicCluster implements GossipListener, PropertiesUser {

    private final Ict ict;
    private Set<TrustedEconomicActor> actors = new HashSet<>();
    private final ECGossipFilter filter = new ECGossipFilter();

    public EconomicCluster(Ict ict) {
        this.ict = ict;
        ict.addGossipListener(this);
    }

    public void addActor(TrustedEconomicActor actor) {
        actors.add(actor);
        filter.watchAddress(actor.getAddress());
    }

    @Override
    public void updateProperties(FinalProperties properties) {
        List<String> list = properties.economicCluster();
        HashSet<TrustedEconomicActor> newActors = new HashSet<>();
        for(String element : list) {
            String address = element.split(":")[0];
            double trust = Double.parseDouble(element.split(":")[1]);
            actors.add(new TrustedEconomicActor(address, trust));
        }

        actors = newActors;
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
