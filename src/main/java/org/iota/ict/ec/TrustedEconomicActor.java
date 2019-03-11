package org.iota.ict.ec;

import org.iota.ict.model.bc.BalanceChange;
import org.iota.ict.model.bundle.Bundle;
import org.iota.ict.model.transaction.Transaction;
import org.iota.ict.model.transfer.Transfer;
import org.iota.ict.utils.Trytes;
import org.iota.ict.utils.crypto.MerkleTree;

import java.util.*;

/**
 * Monitors an economic actor by following its markers. The passively-reading counter-part to {@link ControlledEconomicActor}.
 * */
public class TrustedEconomicActor extends EconomicActor {

    protected double trust;
    protected final Map<String, SubTangle> latest = new HashMap<>();
    protected final Map<String, Set<String>> missingChildren = new HashMap<>();

    public TrustedEconomicActor(String address, double trust) {
        super(address);
        setTrust(trust);
    }

    public void setTrust(double trust) {
        if(trust < 0 || trust > 1)
            throw new IllegalArgumentException("'trust' must be within interval [0,1].");
        this.trust = trust;
    }

    public double getConfidence(Transaction transaction) {
        return latest.containsKey(transaction.hash) ? latest.get(transaction.hash).confidence : 0;
    }

    public double getTrust() {
        return trust;
    }

    public void processTransaction(Transaction transaction) {
        if(missingChildren.keySet().contains(transaction.hash))
            missingChildFound(transaction);
    }

    private synchronized void missingChildFound(Transaction missingChild) {
        Set<String> parents = missingChildren.get(missingChild.hash);
        missingChildren.remove(missingChild.hash);
        SubTangle latestSubTangleOfParents = null;
        for(String parent : parents) {
            SubTangle subTangleOfParent = latest.get(parent);
            if(latestSubTangleOfParents == null || latestSubTangleOfParents.index < subTangleOfParent.index) {
                latestSubTangleOfParents = subTangleOfParent;
            }
        }
        markAsApprovedRecursively(latestSubTangleOfParents, missingChild);
    }

    private synchronized void reportMissingChildren(String parent, String child) {
        if(!missingChildren.containsKey(child))
            missingChildren.put(child, new HashSet<String>());
        missingChildren.get(child).add(parent);
    }

    public void processMarker(Bundle marker) {

        if(!marker.isComplete() || !marker.isStructureValid()) {
            // incomplete or invalid bundle
            return;
        }

        MerkleTree.Signature markerSignature = getMarkerSIgnature(marker);

        if(markerSignature == null || !address.equals(markerSignature.deriveAddress())) {
            // invalid signature
            return;
        }

        Transaction tail = marker.getTail();
        double confidence = decodeConfidence(tail.tag());
        SubTangle subTangle = new SubTangle(markerSignature.deriveIndex(), confidence);
        markAsApprovedRecursively(subTangle, tail);
    }

    private static double decodeConfidence(String trytes) {
        return Trytes.TRYTES.indexOf(trytes.charAt(0)) / 26.0;
    }

    private void markAsApprovedRecursively(SubTangle subTangle, Transaction root) {
        SubTangle before = latest.get(root.hash);
        if(latest.get(root.hash) == null || before.index < subTangle.index) {
            latest.put(root.hash, subTangle);
            markAsApprovedRecursivelyOrReportMissing(subTangle, root.getTrunk(), root.trunkHash(), root.hash);
            markAsApprovedRecursivelyOrReportMissing(subTangle, root.getBranch(), root.branchHash(), root.hash);
        }
    }

    private void markAsApprovedRecursivelyOrReportMissing(SubTangle subTangle, Transaction childOrNull, String childHash, String parentHash) {
        if(childOrNull != null) {
            markAsApprovedRecursively(subTangle, childOrNull);
        } else {
            reportMissingChildren(parentHash, childHash);
        }
    }

    protected MerkleTree.Signature getMarkerSIgnature(Bundle marker) {
        Transfer transfer = new Transfer(marker);
        List<BalanceChange> listOfSingleOutput = new LinkedList<>(transfer.getOutputs());
        if(listOfSingleOutput.size() != 1)
            return null;
        BalanceChange output = listOfSingleOutput.get(0);
        if(!output.address.equals(address))
            return null;
        String messageToSign = messageToSign(marker.getTail().trunkHash(), marker.getTail().branchHash());

        String signatureTrytesConcatenatedWithMerklePath = output.getSignatureOrMessage().replace(Trytes.NULL_HASH, "");
        return MerkleTree.Signature.fromTrytesConcatenatedWithMerklePath(signatureTrytesConcatenatedWithMerklePath, messageToSign);
    }

    private class SubTangle {
        public final int index;
        public final double confidence;

        public SubTangle(int index, double confidence) {
            this.index = index;
            this.confidence = confidence;
        }
    }
}