package org.iota.ict.ec;

import org.iota.ict.ixi.Ixi;
import org.iota.ict.model.bc.BalanceChange;
import org.iota.ict.model.bundle.Bundle;
import org.iota.ict.model.transaction.Transaction;
import org.iota.ict.model.transaction.TransactionBuilder;
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

    public boolean areTanglesCompatible(String hashA, String hashB, String hashC, String hashD) {
        Transaction refA = ixi.findTransactionByHash(hashA);
        Transaction refB = ixi.findTransactionByHash(hashB);
        Transaction refC = ixi.findTransactionByHash(hashC);
        Transaction refD = ixi.findTransactionByHash(hashD);
        return isTangleSolid(merge(merge(refA, refB), merge(refC, refD)));
    }

    public boolean areTanglesCompatible(String hashA, String hashB) {
        Transaction refA = ixi.findTransactionByHash(hashA);
        Transaction refB = ixi.findTransactionByHash(hashB);
        return isTangleSolid(merge(refA, refB));
    }

    private Transaction merge(Transaction refA, Transaction refB) {
        TransactionBuilder builder = new TransactionBuilder();
        builder.trunkHash = refA.hash;
        builder.branchHash = refB.hash;
        Transaction merge = builder.build();
        merge.setTrunk(refA);
        merge.setBranch(refB);
        return merge;
    }

    public boolean isTangleSolid(String rootHash) {
        return isTangleSolid(ixi.findTransactionByHash(rootHash));
    }

    protected boolean isTangleSolid(Transaction root) {
        try {
            return isTangleValid(root.hash, root) && noNegativeBalanceInTangle(root);
        } catch (IncompleteTangleException e) {
            return false;
        }
    }

    protected boolean noNegativeBalanceInTangle(Transaction root) {
        Map<String, BigInteger> balances = calcBalances(root);
        for (Map.Entry<String, BigInteger> entry : balances.entrySet()) {
            if(entry.getValue().compareTo(BigInteger.ZERO) < 0)
                return false;}
        return true;
    }

    protected Map<String, BigInteger> calcBalances(Transaction root) {

        Map<String, BigInteger> balances = new HashMap<>(initialBalances);
        LinkedList<Transaction> toTraverse = new LinkedList<>();
        Set<String> traversed = new HashSet<>();
        toTraverse.add(root);

        while (toTraverse.size() > 0) {
            Transaction current = toTraverse.poll();

            if(traversed.add(current.hash)) {
                if(!current.value.equals(BigInteger.ZERO)) {
                    String address = current.address();
                    balances.put(address, balances.containsKey(address) ? balances.get(address).add(current.value) : current.value);
                }

                Transaction branch = current.getBranch();
                Transaction trunk = current.getTrunk();
                if(branch == null || trunk == null)
                    throw new IncompleteTangleException(branch == null ? current.branchHash() : current.trunkHash());
                toTraverse.add(branch);
                toTraverse.add(trunk);
            }
        }

        return balances;
    }

    public boolean isTangleValid(String rootHash) {
        return isTangleValid(rootHash, ixi.findTransactionByHash(rootHash));
    }

    protected boolean isTangleValid(String rootHash, Transaction root) {
        if(root == null)
            throw new IncompleteTangleException(rootHash);
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
            return isTangleValid(head.trunkHash(), head.getTrunk()) && isTangleValid(head.branchHash(), head.getBranch());
        } catch (IncompleteTangleException incompleteTangleException) {
            dependencyByTransfer.put(head.hash, incompleteTangleException.unavailableTransactionHash);
            throw incompleteTangleException;
        }
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
