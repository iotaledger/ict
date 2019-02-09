package org.iota.ict.ec;

import org.iota.ict.model.Bundle;
import org.iota.ict.model.Transaction;
import org.iota.ict.model.Transfer;

import java.util.HashSet;
import java.util.Set;

public class TrustedEconomicActor extends EconomicActor {

    protected double trust;
    protected final Set<String> approved = new HashSet<>();

    public TrustedEconomicActor(String address, double trust) {
        super(address);
        setTrust(trust);
    }

    public void setTrust(double trust) {
        if(trust < 0 || trust > 1)
            throw new IllegalArgumentException("'trust' must be within interval [0,1].");
        this.trust = trust;
    }

    public boolean approvesTransaction(Transaction transaction) {
        return approved.contains(transaction.hash);
    }

    public double getTrust() {
        return trust;
    }

    public void processMarker(Bundle marker) {

        if(!marker.isStructureValid()) {
            // incomplete bundle
            return;
        }

        if(!isMarkerSignatureValid(marker)) {
            // invalid signature
            return;
        }

        markAsApprovedRcursively(marker.getHead());
    }

    private void markAsApprovedRcursively(Transaction root) {
        if(root != null && approved.add(root.hash)) {
            markAsApprovedRcursively(root.getTrunk());
            markAsApprovedRcursively(root.getBranch());
        }
    }

    protected boolean isMarkerSignatureValid(Bundle marker) {
        Transfer transfer = new Transfer(marker);
        System.err.println(marker.getHead().address().equals(address) + " " + (transfer.getInputs().size() == 1) + " "  + (transfer.getOutputs().size() == 0)  + " " + transfer.areSignaturesValid());
        return marker.getHead().address().equals(address) && transfer.getInputs().size() == 1 && transfer.getOutputs().size() == 0 && transfer.areSignaturesValid();
    }
}