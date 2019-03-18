package org.iota.ict.ec;

import org.iota.ict.ixi.Ixi;
import org.iota.ict.model.bundle.Bundle;
import org.iota.ict.model.transaction.Transaction;
import org.iota.ict.utils.crypto.MerkleTree;

import java.util.*;

public class AutonomousEconomicActor extends ControlledEconomicActor {

    private final Ixi ixi;
    private final LedgerValidator ledgerValidator;
    private Map<String, Double> confidenceByMarkedTangle = new HashMap<>();
    private final EconomicCluster economicCluster;

    public AutonomousEconomicActor(Ixi ixi, EconomicCluster economicCluster, MerkleTree merkleTree, int keyIndex) {
        super(merkleTree, keyIndex);
        this.ixi = ixi;
        this.economicCluster = economicCluster;
        this.ledgerValidator = new LedgerValidator(ixi);
    }

    protected void tick() {
        String mostContriversTangle = economicCluster.findMostControversTangle();
        if(mostContriversTangle != null) {
            adjustConfidence(mostContriversTangle, determineConfidence(mostContriversTangle));
        }
    }

    protected double determineConfidence(String tangle) {

        if(!confidenceByMarkedTangle.containsKey(tangle)) {
            String ref1 = tangle.substring(0, 81);
            String ref2 = tangle.substring(81);
            if(ledgerValidator.areTanglesCompatible(ref1, ref2)) {
                // tangle invalid
                return 0;
            }
        }

        // TODO: do not consider ALL tangles, does not scale well
        List<String> tangles = new LinkedList<>(economicCluster.getAllTangles());
        return createConfidenceCalculator(tangles).confidenceOf(tangle);
    }

    protected ConfidenceCalculator createConfidenceCalculator(List<String> tangles) {

        assert tangles.size() > 0;

        Set<ConfidenceCalculator.Conflict> conflicts = findAllConflicts(tangles);

        double[] initialProbabilities = new double[tangles.size()];

        for(int i = 0; i < tangles.size(); i++) {
            String someTangle = tangles.get(i);
            String ref1 = someTangle.substring(0, 81);
            String ref2 = someTangle.substring(81);
            double confidenceRef1 = economicCluster.determineApprovalConfidence(ref1);
            double confidenceRef2 = economicCluster.determineApprovalConfidence(ref2);
            initialProbabilities[i] = Math.min(confidenceRef1, confidenceRef2);
        }

        return new ConfidenceCalculator(tangles, conflicts, initialProbabilities);
    }

    protected Set<ConfidenceCalculator.Conflict> findAllConflicts(List<String> tangles) {
        Set<ConfidenceCalculator.Conflict> conflicts = new HashSet<>();
        for(int i = 0; i < tangles.size(); i++) {
            String tangleI = tangles.get(i);
            for(int j = i+1; j < tangles.size(); j++) {
                String tangleJ = tangles.get(j);
                if(!ledgerValidator.areTanglesCompatible(tangleI.substring(0, 81), tangleI.substring(81), tangleJ.substring(0, 81), tangleJ.substring(81)))
                    conflicts.add(new ConfidenceCalculator.Conflict(tangleI, tangleJ));
            }
        }
        return conflicts;
    }

    protected void adjustConfidence(String tangle, double newConfidence) {
        System.err.println("adjusting confidence for " + tangle + " to " + newConfidence);
        boolean shouldIssueNewMarker = !confidenceByMarkedTangle.containsKey(tangle) || shouldIssueMarkerToUpdateConfidence(confidenceByMarkedTangle.get(tangle), newConfidence);
        if(shouldIssueNewMarker) {
            confidenceByMarkedTangle.put(tangle, newConfidence);
            String trunk = tangle.substring(0, 81);
            String branch = tangle.substring(81);
            Bundle marker = buildMarker(trunk, branch, newConfidence);
            for(Transaction t : marker.getTransactions())
                ixi.submit(t);
        }
    }

    private static boolean shouldIssueMarkerToUpdateConfidence(double currentConfidence, double newConfidence) {
        String currentEncodedConfidence = encodeConfidence(currentConfidence);
        String newEncodedConfidence = encodeConfidence(newConfidence);
        return !currentEncodedConfidence.equals(newEncodedConfidence);
    }
}
