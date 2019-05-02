package org.iota.ict.model.transfer;

import org.iota.ict.model.bundle.Bundle;
import org.iota.ict.model.bc.BalanceChange;
import org.iota.ict.model.transaction.Transaction;
import org.iota.ict.utils.Constants;
import org.iota.ict.utils.Trytes;
import org.iota.ict.utils.crypto.SignatureSchemeImplementation;

import java.math.BigInteger;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A {@link Transfer} is a {@link Bundle} which transfers value. Every transfer can be interpreted as a bundle and every
 * bundle as transfer. They provide different views on the same thing. The bundle perspective puts its focus on the
 * transactions and how they are linked together, while the transfer abstracts from the individual transactions and
 * groups them together into {@link BalanceChange} objects.
 * <p>
 * Compared to {@link Bundle}, this class also provides additional functionality which is only useful in the context of
 * value transfers but not for general data bundles (which do not actually transfer a value). Examples are {@link #getSecurityLevel()}
 * and {@link #areSignaturesValid()}.
 *
 * @see Bundle as generalization.
 * @see TransferBuilder to create new transfers.
 */

public class Transfer {

    private final String bundleHash;
    private final Set<BalanceChange> inputs;
    private final Set<BalanceChange> outputs;
    private final int securityLevel;
    private SignatureVerificationState signatureVerificationState = SignatureVerificationState.SIGNATURE_NOT_VERIFIED;

    public Transfer(Bundle bundle) {
        bundleHash = bundle.getHash();
        BalanceChangeCollector collector = new BalanceChangeCollector(bundle);
        inputs = collector.inputs;
        outputs = collector.outputs;
        securityLevel = calcSecurityLevel();
    }

    public boolean isValid() {
        return areSignaturesValid() && isSumZero();
    }

    private boolean isSumZero() {
        BigInteger sum = BigInteger.ZERO;
        for (BalanceChange change : inputs) {
            sum = sum.add(change.value);
        }
        for (BalanceChange change : outputs) {
            sum = sum.add(change.value);
        }
        return sum.compareTo(BigInteger.ZERO) == 0;
    }

    public boolean areSignaturesValid() {
        if (signatureVerificationState == SignatureVerificationState.SIGNATURE_NOT_VERIFIED) {
            signatureVerificationState = verifyAllInputs() ? SignatureVerificationState.SIGNATURE_VALID : SignatureVerificationState.SIGNATURE_INVALID;
        }
        return signatureVerificationState == SignatureVerificationState.SIGNATURE_VALID;
    }

    private boolean verifyAllInputs() {
        boolean allInputsValid = true;
        for (BalanceChange input : inputs) {
            if (!verifyInput(input)) {
                allInputsValid = false;
                break;
            }
        }
        return allInputsValid;
    }

    private boolean verifyInput(BalanceChange input) {
        if(securityLevel == 0)
            return false;
        // security level must equal amount of fragments for level 1 and 2
        if((securityLevel == 1 || securityLevel == 2) && securityLevel != input.getAmountOfSignatureOrMessageFragments())
            return false;
        if((securityLevel == 3) && input.getAmountOfSignatureOrMessageFragments() < 3)
            return false;
        return isSignatureValid(input);
    }

    private boolean isSignatureValid(BalanceChange input) {
        try {
            for(int i = 0; i < input.getAmountOfSignatureOrMessageFragments(); i++) {
                String signatureFragmentTrytes = input.getSignatureOrMessageFragment(i);
                String bundleHashFragment = bundleHash.substring((i%3)*27, (i%3+1)*27);
                SignatureSchemeImplementation.Signature signatureFragment = new SignatureSchemeImplementation.Signature(signatureFragmentTrytes, bundleHashFragment);
                String signedAddress = signatureFragment.deriveAddress();
                if(!signedAddress.equals(input.address))
                    return false;
            }
            return true;
        } catch (Throwable t) {
            t.printStackTrace();
            return false;
        }
    }

    static int calcSecurityLevel(String bundleHash) {
        // TODO: use in future, requires lots of proof-of-work (100^sl tries)
        for (int i = 0; i < 3; i++)
            if (Trytes.sumTrytes(bundleHash.substring(27 * i, 27 * i + 27)) != 0)
                return i;
        return 3;
    }


    int calcSecurityLevel() {
        // TODO: replace with hash based calculation in the future
        for(BalanceChange input : inputs)
            return Math.min(3, input.getAmountOfSignatureOrMessageFragments());
        return 0;
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
        private org.iota.ict.model.bc.BalanceChangeCollector currentBuilder;
        private State state = null;

        private BalanceChangeCollector(Bundle bundle) {
            List<Transaction> transactionsReverseOrder = bundle.getTransactions();
            Collections.reverse(transactionsReverseOrder);
            for (Transaction t : transactionsReverseOrder)
                assignTransactionToInputOrOutput(t);
            completeCurrentBuilding();
        }

        private void assignTransactionToInputOrOutput(Transaction t) {

            boolean valueNegative = t.value.compareTo(BigInteger.ZERO) < 0;
            boolean valueZero = t.value.compareTo(BigInteger.ZERO) == 0;
            boolean canAppendToBuilder = currentBuilder != null && valueZero && currentBuilder.getAddress().equals(t.address());

            // transaction is part of current change
            if (canAppendToBuilder)
                currentBuilder.append(t);

            // transaction opens new change, prepare by closing current one
            if (!canAppendToBuilder && currentBuilder != null)
                completeCurrentBuilding();

            if (currentBuilder == null) {
                // transaction opens new change
                currentBuilder = new org.iota.ict.model.bc.BalanceChangeCollector(t);
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

    private enum SignatureVerificationState {
        SIGNATURE_VALID, SIGNATURE_INVALID, SIGNATURE_NOT_VERIFIED
    }
}
