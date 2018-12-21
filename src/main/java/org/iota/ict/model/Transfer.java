package org.iota.ict.model;

import org.iota.ict.utils.Trytes;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;

public class Transfer {

    private final Set<BalanceChange> inputs;
    private final Set<BalanceChange> outputs;
    private final int securityLevel;

    public Transfer(Set<BalanceChange> changes, int securityLevel) {
        ensureSumIsZero(changes);
        inputs = filterOutChangesWithNegativeValue(changes);
        outputs = new HashSet<>(changes);
        outputs.removeAll(inputs);
        this.securityLevel = securityLevel;
    }

    public Transfer(Bundle bundle) {
        securityLevel = calcSecurityLevel(bundle.getHash());
        BalanceChangeCollector collector = new BalanceChangeCollector(bundle);
        inputs = collector.inputs;
        outputs = collector.outputs;
    }

    private static void ensureSumIsZero(Iterable<BalanceChange> changes) {
        BigInteger sum = BigInteger.ZERO;
        for (BalanceChange change : changes) {
            sum = sum.add(change.value);
        }
        if (sum.compareTo(BigInteger.ZERO) != 0)
            throw new IllegalArgumentException("Total sum of changes must be 0 but is '" + sum.toString() + "'.");
    }

    void validateSignatures() {

    }

    static Set<BalanceChange> filterOutChangesWithNegativeValue(Iterable<BalanceChange> changes) {
        Set<BalanceChange> inputs = new HashSet<>();
        for (BalanceChange change : changes)
            if (change.value.compareTo(BigInteger.ZERO) < 0)
                inputs.add(change);
        return inputs;
    }

    static int calcSecurityLevel(String bundleHash) {
        for (int i = 0; i < 3; i++)
            if (Trytes.sumTrytes(bundleHash.substring(27 * i, 27 * i + 27)) != 0)
                return i;
        return 3;
    }

    public Bundle buildBundle() {
        BundleBuilder bundleBuilder = new BundleBuilder();
        for (BalanceChange change : inputs)
            change.appendToBundleBuilder(bundleBuilder);
        for (BalanceChange change : outputs)
            change.appendToBundleBuilder(bundleBuilder);
        return bundleBuilder.build();
    }

    public int getSecurityLevel() {
        return securityLevel;
    }

    public Set<BalanceChange> getInputs() {
        return new HashSet<>(inputs);
    }

    public Set<BalanceChange> getOutputs() {
        return new HashSet<>(outputs);
    }

    /**
     * Helper class to modularize the input/output filtering process. Might seem unnecessarily complex but allowed to avoid
     * code duplication: the code for input collection and output collection were pretty similar but parts could not be reused
     * because this would have violated clean code guidelines (would require either too many parameters or flag parameters).
     * By moving the process into a separate object with attributes and a state, this could be avoided.
     */
    private static class BalanceChangeCollector {

        private final Set<BalanceChange> inputs = new HashSet<>();
        private final Set<BalanceChange> outputs = new HashSet<>();
        private BalanceChangeBuilder currentBuilder;
        private State state = null;

        private BalanceChangeCollector(Bundle bundle) {
            for (Transaction t : bundle.getTransactions())
                assignTransactionToInputOrOutput(t);
            completeCurrentBuilding();
        }

        private void assignTransactionToInputOrOutput(Transaction t) {

            boolean valueNegative = t.value.compareTo(BigInteger.ZERO) < 0;
            boolean valueZero = t.value.compareTo(BigInteger.ZERO) == 0;
            boolean canAppendToBuilder = currentBuilder != null && valueZero && currentBuilder.address.equals(t.address);

            // transaction is part of current change
            if (canAppendToBuilder)
                currentBuilder.append(t);

            // transaction opens new change, prepare by closing current one
            if (!canAppendToBuilder && currentBuilder != null)
                completeCurrentBuilding();

            if (currentBuilder == null) {
                // transaction opens new change
                currentBuilder = new BalanceChangeBuilder(t);
                state = valueNegative ? State.BUILDING_INPUT : State.BUILDING_OUTPUT;
            }
        }

        private void completeCurrentBuilding() {
            Set<BalanceChange> set = state == State.BUILDING_INPUT ? inputs : outputs;
            set.add(currentBuilder.build());
            currentBuilder = null;
        }

        private enum State {
            BUILDING_INPUT, BUILDING_OUTPUT
        }
    }
}
