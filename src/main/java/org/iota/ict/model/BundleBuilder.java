package org.iota.ict.model;

import java.util.LinkedList;
import java.util.List;

/**
 * Similar to {@link TransactionBuilder}, {@link BundleBuilder} makes it possible to create a {@link Bundle}.
 * Bundles are read from head to tail but created from tail to head. This is why it makes sense to have a dedicated class
 * for this purpose.
 */
public class BundleBuilder {

    private final LinkedList<TransactionBuilder> tailToHead = new LinkedList<>();

    public void append(List<TransactionBuilder> unfinishedTransactionsFromTailToHead) {
        for (TransactionBuilder unfinishedTransaction : unfinishedTransactionsFromTailToHead)
            append(unfinishedTransaction);
    }

    public void append(TransactionBuilder unfinishedTransaction) {
        tailToHead.add(unfinishedTransaction);
    }

    public Bundle build() {
        if(tailToHead.size() == 0)
            throw new IllegalStateException("Cannot build: bundle is empty (0 transactions).");
        setFlags();
        Transaction head = buildTrunkLinkedChainAndReturnHead();
        return tailToHead.size() > 0 ? new Bundle(head) : null;
    }

    private Transaction buildTrunkLinkedChainAndReturnHead() {

        Transaction lastTransaction = null;
        for (int i = 0; i < tailToHead.size(); i++) {
            boolean isFirst = i == 0;
            TransactionBuilder unfinished = tailToHead.get(i);
            if (!isFirst)
                unfinished.trunkHash = lastTransaction.hash;
            Transaction currentTransaction = unfinished.build();
            currentTransaction.trunk = lastTransaction;
            lastTransaction = currentTransaction;
        }

        return lastTransaction;
    }

    private void setFlags() {
        for (TransactionBuilder unfinished : tailToHead) {
            unfinished.isBundleHead = false;
            unfinished.isBundleTail = false;
        }
        tailToHead.getFirst().isBundleTail = true;
        tailToHead.getLast().isBundleHead = true;
    }

    public List<TransactionBuilder> getTailToHead() {
        return new LinkedList<>(tailToHead);
    }
}
