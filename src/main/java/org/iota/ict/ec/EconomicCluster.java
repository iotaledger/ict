package org.iota.ict.ec;

import org.iota.ict.Ict;
import org.iota.ict.eee.Environment;
import org.iota.ict.ixi.Ixi;
import org.iota.ict.model.bundle.Bundle;
import org.iota.ict.model.transaction.Transaction;
import org.iota.ict.network.gossip.GossipEvent;
import org.iota.ict.network.gossip.GossipFilter;
import org.iota.ict.network.gossip.GossipListener;
import org.iota.ict.utils.Constants;
import org.iota.ict.utils.properties.FinalProperties;
import org.iota.ict.utils.properties.PropertiesUser;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EconomicCluster implements GossipListener, PropertiesUser {

    private final Ixi ict;
    private Set<TrustedEconomicActor> actors = new HashSet<>();
    private final ECGossipFilter filter = new ECGossipFilter();

    public EconomicCluster(Ict ict) {
        this.ict = ict;
        ict.addListener(this);
    }

    public void addActor(TrustedEconomicActor actor) {
        actors.add(actor);
        filter.watchAddress(actor.getAddress());
    }

    public Set<String> getAllTangles() {
        Set<String> allTangles = new HashSet<>();
        for(TrustedEconomicActor actor : actors)
            allTangles.addAll(actor.getMarkedTangles());
        return allTangles;
    }

    @Override
    public void updateProperties(FinalProperties properties) {
        Set<String> set = properties.economicCluster();
        HashSet<TrustedEconomicActor> newActors = new HashSet<>();
        for(String element : set) {
            String[] split = element.split(":");
            String address = split[0];
            double trust = Double.parseDouble(split[1]);
            actors.add(new TrustedEconomicActor(address, trust));
        }

        actors = newActors;
    }

    public double determineApprovalConfidence(String transactionHash) {
        double maxAbsTrust = calcMaxAbsTrust();
        double absTrust = 0;
        for(TrustedEconomicActor actor : actors) {
            absTrust += actor.getTrust() * actor.getConfidence(transactionHash);
        }
        return absTrust / maxAbsTrust;
    }

    private double calcMaxAbsTrust() {
        double trustSUm = 0;
        for(TrustedEconomicActor actor : actors) {
            trustSUm += actor.getTrust();
        }
        return trustSUm;
    }

    @Override
    public void onReceive(GossipEvent event) {
        Transaction transaction = event.getTransaction();

        if(filter.passes(transaction)) {

            Bundle possiblyMarker = new Bundle(transaction);
            if(!possiblyMarker.isStructureValid())
                return;

            for(TrustedEconomicActor actor : actors) {
                if(actor.getAddress().equals(transaction.address()))
                    actor.processMarker(possiblyMarker);
            }
        }

        for(TrustedEconomicActor actor : actors) {
            actor.processTransaction(transaction);
        }
    }

    @Override
    public Environment getEnvironment() {
        return Constants.Environments.GOSSIP;
    }

    private class ECGossipFilter extends GossipFilter {

        @Override
        public boolean passes(Transaction transaction) {
            return transaction.isBundleHead && super.passes(transaction);
        }
    }
}
