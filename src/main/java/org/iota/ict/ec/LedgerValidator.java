package org.iota.ict.ec;

import org.iota.ict.ixi.Ixi;
import org.iota.ict.model.bc.BalanceChange;
import org.iota.ict.model.bundle.Bundle;
import org.iota.ict.model.transaction.Transaction;
import org.iota.ict.model.transfer.Transfer;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LedgerValidator {

    protected final Ixi ixi;

    protected final Map<String, String> dependencyByTransfer = new HashMap<>();
    protected final Set<String> invalidTransfers = new HashSet<>(), validTransfers = new HashSet<>();

    LedgerValidator(Ixi ixi) {
        this.ixi = ixi;
        validTransfers.add(Transaction.NULL_TRANSACTION.hash);
    }

    public boolean isTangleValid(String hash) {
        return isTangleValid(hash, ixi.findTransactionByHash(hash));
    }

    protected boolean isTangleValid(String hash, Transaction root) {

        if(root == null) {
            throw new IncompleteTangleException(hash);
        }

        if(validTransfers.contains(root.hash))
            return true;
        if(invalidTransfers.contains(root.hash))
            return false;
        checkForMissingDependency(root.hash);

        boolean isValid = (!root.isBundleHead || (root.isBundleTail && root.value.compareTo(BigInteger.ZERO) == 0))
                ? isTangleValid(root.branchHash(), root.getBranch()) && isTangleValid(root.trunkHash(), root.getTrunk())
                : isBundleValid(root);

        (isValid ? validTransfers : invalidTransfers).add(root.hash);

        return isValid;
    }

    protected void checkForMissingDependency(String rootHash) {
        if(dependencyByTransfer.containsKey(rootHash)) {
            String dependency = dependencyByTransfer.get(rootHash);
            if(ixi.findTransactionByHash(dependency) == null) {
                throw new IncompleteTangleException(dependency);
            } else {
                dependencyByTransfer.remove(rootHash);
            }
        }
    }

    protected boolean isBundleValid(Transaction head) {

        Transfer transfer = new Transfer(new Bundle(head));
        if(!transfer.areSignaturesValid()) {
            return false;
        }

        try {
            return isTangleValid(head.trunkHash(), head.getTrunk()) && isTangleValid(head.branchHash(), head.getBranch()) && verifyFunds(transfer);
        } catch (IncompleteTangleException incompleteTangleException) {
            dependencyByTransfer.put(head.hash, incompleteTangleException.unavailableTransactionHash);
            throw incompleteTangleException;
        }
    }

    protected boolean verifyFunds(Transfer transfer) {
        for(BalanceChange input : transfer.getInputs())
            if(!verifyFunds(input.getAddress(), input.getValue()))
                return false;
        return true;
    }

    protected boolean verifyFunds(String address, BigInteger value) {
        return true; // TODO
    }

    protected static class IncompleteTangleException extends RuntimeException {
        protected final String unavailableTransactionHash;

        IncompleteTangleException(String unavailableTransactionHash) {
            this.unavailableTransactionHash = unavailableTransactionHash;
        }
    }
}
