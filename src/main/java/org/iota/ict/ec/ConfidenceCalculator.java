package org.iota.ict.ec;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * This class allows to calculate the confidence for a tangle based on the confidence of others and the conflict
 * relation among them.
 *
 * The confidence represents the probability a Tangle will be confirmed assuming that we randomly pick Tangles of the
 * available ones and let them confirm until every Tangle is either confirmed or incompatible with the confirmed Tangles.
 *
 * To determine the confidence of a Tangle X, we iterate over all Tangles Y and assume that Y confirmed. Under this
 * assumption we can now ignore all Tangles incompatible with Y and determine the confidence of X in that subset recursively.
 *
 * CONFIDENCE(X) = SUM [over all Y] P(Y) * CONFIDENCE(X|Y)
 * */
public class ConfidenceCalculator {

    private List<String> tangles;
    private Set<Conflict> conflicts;
    private final double confidences[];

    public ConfidenceCalculator(List<String> tangles, Set<Conflict> conflicts) {
        this.tangles = new LinkedList<>(tangles);
        this.conflicts = conflicts;

        for(String tangle : tangles)
            if(amountOfConflicts(tangle) == 0)
                this.tangles.remove(tangle);

        double pSum = 0;
        confidences = new double[tangles.size()];
        for(int i = 0; i < confidences.length; i++) {
            confidences[i] = calcConfidence(tangles.get(i));
            pSum += confidences[i];
        }
        for(int i = 0; i < confidences.length; i++) {
            confidences[i] /= pSum;
        }
    }

    /**
     * @param tangle The Tangle for which to estimate the initial probability.
     * @return initial probability of the respective Tangle confirming first.
     * */
    private double initialProbabilityOf(String tangle) {
        return amountOfConflicts(tangle) / 2.0 / conflicts.size();
    }

    /**
     * Accesses the confidence previously calculated when creating this {@link ConfidenceCalculator} object.
     * @return the confidence of the Tangle under the assumptions made by this {@link ConfidenceCalculator} object.
     * */
    public double confidenceOf(String tangle) {
        return confidences[tangles.indexOf(tangle)];
    }

    /**
     * @param tangle The Tangle for which to calculate the confidence.
     * @return the confidence of the Tangle under the assumptions made by this {@link ConfidenceCalculator} object.
     * */
    private double calcConfidence(String tangle) {

        if(amountOfConflicts(tangle) == 0)
            // Tangle is compatible with all others -> can be merged with any of them.
            return 1;

        double p = 0;

        for (String otherTangle : compatible(tangle)) {
            p += initialProbabilityOf(otherTangle) * (tangle.equals(otherTangle) ? 1 : new ConfidenceCalculator(compatible(otherTangle), conflicts).confidenceOf(tangle));
        }
        return p;
    }

    /**
     * @param tangle The Tangle for which to find all compatible Tangles.
     * @return All Tangles in {@link #tangles} compatible with the respective tangle.
     * */
    private List<String> compatible(String tangle) {
        List<String> compatible = new LinkedList<>(tangles);
        for(Conflict conflict : conflicts)
            if(conflict.appliesTo(tangle, tangles))
                compatible.remove(conflict.getOther(tangle));
        return compatible;
    }

    /**
     * @param tangle The Tangle for which to count the amount of conflicts.
     * @return The amount of Tangles in {@link #tangles} this Tangle stands in conflict to.
     * */
    private int amountOfConflicts(String tangle) {
        int amountOfConflicts = 0;
        for(Conflict conflict : conflicts)
            if(conflict.appliesTo(tangle, tangles))
                amountOfConflicts++;
        return amountOfConflicts;
    }



    /**
     * This class models that two tangles are in conflict with each other. They cannot both confirm.
     * */
    public static class Conflict {
        private final String tangle1;
        private final String tangle2;

        /**
         * @param tangle1 First Tangle to be in conflict with the second.
         * @param tangle2 Second Tangle to be in conflict with the first.
         * */
        Conflict(String tangle1, String tangle2) {
            this.tangle1 = tangle1;
            this.tangle2 = tangle2;
        }

        /**
         * @param tangle The Tangle for which to determine whether this conflict applies to.
         * @param tangles The context of existent Tangles. Both Tangles of the conflict must be included to let this conflict apply.
         * @return Whether this conflict is relevant for a specific Tangle in some context of existing Tangles.
         * */
        boolean appliesTo(String tangle, List<String> tangles) {
            return (tangle1.equals(tangle) || tangle2.equals(tangle)) && tangles.contains(tangle1) && tangles.contains(tangle2);
        }

        /**
         * @param tangle One of the two Tangles this conflict applies to.
         * @return The other Tangle which stands in conflict to the Tangle passed as parameter.
         * */
        String getOther(String tangle) {
            if(tangle.equals(tangle1) || tangle.equals(tangle2))
                return tangle1.equals(tangle) ? tangle2 : tangle1;
            throw new IllegalArgumentException();
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Conflict && obj.toString().equals(toString());
        }

        @Override
        public int hashCode() {
            return toString().hashCode();
        }

        @Override
        public String toString() {
            return tangle1.compareTo(tangle2) < 0 ? tangle1 + tangle2 : tangle2 + tangle1;
        }
    }
}