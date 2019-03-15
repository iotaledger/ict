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

    protected List<SubTangle> subTanglesOrderedByDescendingConfidence = new LinkedList<>();
    protected double trust;

    public TrustedEconomicActor(String address, double trust) {
        super(address);
        setTrust(trust);
    }

    public void setTrust(double trust) {
        if(trust < 0 || trust > 1)
            throw new IllegalArgumentException("'trust' must be within interval [0,1].");
        this.trust = trust;
    }

    public double getConfidence(String transactionHash) {
        for(SubTangle subTangle : subTanglesOrderedByDescendingConfidence) {
            if(subTangle.references(transactionHash))
                return subTangle.getConfidence();
        }
        return 0;
    }

    public double getTrust() {
        return trust;
    }

    public void processTransaction(Transaction transaction) {
        for(SubTangle subTangle : subTanglesOrderedByDescendingConfidence)
            subTangle.processTransaction(transaction);
    }

    public void processMarker(Bundle marker) {
        try {
            SubTangle existingSubTangle = findSubTangleDirectlyReferencedBy(marker.getTail());
            if(existingSubTangle != null) {
                existingSubTangle.update(marker);
            } else {
                subTanglesOrderedByDescendingConfidence.add(new SubTangle(marker));
            }
            Collections.sort(subTanglesOrderedByDescendingConfidence);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private SubTangle findSubTangleDirectlyReferencedBy(Transaction transaction) {
        for(SubTangle subTangle : subTanglesOrderedByDescendingConfidence) {
            if(subTangle.isDirectlyReferencedBy(transaction)) {
                return subTangle;
            }
        }
        return null;
    }

    private static double decodeConfidence(String trytes) {
        return Trytes.TRYTES.indexOf(trytes.charAt(0)) / 26.0;
    }

    protected MerkleTree.Signature getMarkerSignature(Bundle marker) {
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

    protected class SubTangle implements Comparable<SubTangle> {

        protected int index = -1;
        protected double confidence;
        protected final String referencedTransaction1, referencedTransaction2;
        protected final Set<String> referenced = new HashSet<>();
        protected final Set<String> missing = new HashSet<>();

        protected SubTangle(Bundle marker) {
            Transaction tail = marker.getTail();
            referencedTransaction1 = tail.branchHash();
            referencedTransaction2 = tail.trunkHash();
            update(marker);
            markAsApprovedRecursively(tail.getBranch());
            markAsApprovedRecursively(tail.getTrunk());
        }

        protected int getIndex() {
            return index;
        }

        protected double getConfidence() {
            return confidence;
        }

        protected void update(Bundle marker) {

            Transaction tail = marker.getTail();
            if(!isDirectlyReferencedBy(tail))
                throw new IllegalArgumentException("Marker does not belong this subtangle.");

            MerkleTree.Signature markerSignature = getMarkerSignature(marker);
            if(markerSignature == null || !address.equals(markerSignature.deriveAddress()))
                throw new IllegalArgumentException("Marker signature is invalid.");

            if(this.index < markerSignature.deriveIndex()) {
                this.index = markerSignature.deriveIndex();
                this.confidence = decodeConfidence(tail.tag());
            }
        }

        private boolean isDirectlyReferencedBy(Transaction transaction) {
            return (transaction.branchHash().equals(referencedTransaction1) || transaction.branchHash().equals(referencedTransaction2))
                    && (transaction.trunkHash().equals(referencedTransaction1) || transaction.trunkHash().equals(referencedTransaction2));
        }

        protected boolean references(String transactionHash) {
            return referenced.contains(transactionHash) || missing.contains(transactionHash);
        }

        protected void processTransaction(Transaction transaction) {
            if(missing.contains(transaction.hash))
                missingTransactionFound(transaction);
        }

        protected synchronized void missingTransactionFound(Transaction missingTransaction) {
            missing.remove(missingTransaction.hash);
            if(referenced.add(missingTransaction.hash))
                markAsApprovedRecursively(missingTransaction);
        }

        protected void markAsApprovedRecursively(Transaction root) {
            if(referenced.add(root.hash)) {
                markAsApprovedRecursivelyOrReportMissing(root.getTrunk(), root.trunkHash());
                markAsApprovedRecursivelyOrReportMissing(root.getBranch(), root.branchHash());
            }
        }

        protected void markAsApprovedRecursivelyOrReportMissing(Transaction transactionOrNull, String transactionHash) {
            if(transactionOrNull != null) {
                markAsApprovedRecursively(transactionOrNull);
            } else {
                missing.add(transactionHash);
            }
        }

        @Override
        public int compareTo(SubTangle subTangle) {
            return Integer.compare(index, subTangle.index);
        }
    }
}