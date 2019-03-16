package org.iota.ict.ec;

import org.iota.ict.ixi.Ixi;
import org.iota.ict.model.bc.BalanceChange;
import org.iota.ict.model.bundle.Bundle;
import org.iota.ict.model.transaction.Transaction;
import org.iota.ict.model.transfer.Transfer;

import java.math.BigInteger;
import java.util.*;

public class LedgerValidator {

    protected final Ixi ixi;

    protected final Map<String, BigInteger> initialBalances = new HashMap<>();
    protected final Map<String, String> dependencyByTransfer = new HashMap<>();
    protected final Set<String> invalidTransfers = new HashSet<>(), validTransfers = new HashSet<>();

    LedgerValidator(Ixi ixi) {
        this.ixi = ixi;
        validTransfers.add(Transaction.NULL_TRANSACTION.hash);
    }

    public void changeInitialBalance(String address, BigInteger toAdd) {
        initialBalances.put(address, initialBalances.containsKey(address) ? initialBalances.get(address).add(toAdd) : toAdd);
    }

    public boolean isTangleValid(String hash) {
        return isTangleValid(hash, ixi.findTransactionByHash(hash));
    }

    protected boolean isTangleValid(String hash, Transaction root) {
        if(root == null)
            throw new IncompleteTangleException(hash);
        if(validTransfers.contains(root.hash))
            return true;
        if(invalidTransfers.contains(root.hash))
            return false;
        checkForMissingDependency(root.hash);
        return validateTangle(root);
    }

    protected boolean validateTangle(Transaction root) {
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
        if(!transfer.isValid()) {
            return false;
        }

        try {
            return isTangleValid(head.trunkHash(), head.getTrunk()) && isTangleValid(head.branchHash(), head.getBranch()) && verifyFunds(head, transfer);
        } catch (IncompleteTangleException incompleteTangleException) {
            dependencyByTransfer.put(head.hash, incompleteTangleException.unavailableTransactionHash);
            throw incompleteTangleException;
        }
    }

    protected boolean verifyFunds(Transaction root, Transfer transfer) {
        for(BalanceChange input : transfer.getInputs())
            if(sumBalanceOfAddress(root, input.getAddress()).compareTo(BigInteger.ZERO) < 0)
                return false;
        return true;
    }

    protected static class IncompleteTangleException extends RuntimeException {
        protected final String unavailableTransactionHash;

        IncompleteTangleException(String unavailableTransactionHash) {
            super(unavailableTransactionHash);
            this.unavailableTransactionHash = unavailableTransactionHash;
        }
    }

    public static BigInteger sumBalanceOfAddress(Transaction root, String address) {
        BigInteger sum = BigInteger.ZERO;

        Set<Transaction> traversed = new HashSet<>();
        LinkedList<Transaction> toTraverse = new LinkedList<>();
        toTraverse.add(root);

        Transaction current;
        while ((current = toTraverse.poll()) != null) {
            if(!traversed.add(current))
                continue;
            toTraverse.add(current.getBranch());
            toTraverse.add(current.getTrunk());

            if(current.address().equals(address))
                sum = sum.add(current.value);
        }

        return sum;
    }
}
