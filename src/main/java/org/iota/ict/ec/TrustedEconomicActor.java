package org.iota.ict.ec;

import org.iota.ict.model.bc.BalanceChange;
import org.iota.ict.model.bundle.Bundle;
import org.iota.ict.model.transaction.Transaction;
import org.iota.ict.model.transfer.Transfer;
import org.iota.ict.utils.crypto.SignatureSchemeImplementation;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
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

        if(!marker.isComplete() || !marker.isStructureValid()) {
            // incomplete or invalid bundle
            return;
        }

        if(!isMarkerSignatureValid(marker)) {
            // invalid signature
            return;
        }

        markAsApprovedRcursively(marker.getTail());
    }

    private void markAsApprovedRcursively(Transaction root) {
        if(root != null && approved.add(root.hash)) {
            // todo approve referenced transactions that are being received later
            markAsApprovedRcursively(root.getTrunk());
            markAsApprovedRcursively(root.getBranch());
        }
    }

    protected boolean isMarkerSignatureValid(Bundle marker) {
        Transfer transfer = new Transfer(marker);
        List<BalanceChange> listOfSingleOutput = new LinkedList<>(transfer.getOutputs());
        if(listOfSingleOutput.size() != 1)
            return false;
        BalanceChange output = listOfSingleOutput.get(0);
        if(!output.address.equals(address))
            return false;
        String messageToSign = messageToSign(marker.getTail().trunkHash(), marker.getTail().branchHash());
        SignatureSchemeImplementation.Signature signature = new SignatureSchemeImplementation.Signature(output.signatureOrMessage, messageToSign);
        return address.equals(signature.deriveAddress());
   }
}