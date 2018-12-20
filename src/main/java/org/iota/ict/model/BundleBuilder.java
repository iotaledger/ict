package org.iota.ict.model;

import java.util.LinkedList;
import java.util.List;

public class BundleBuilder {

    private final LinkedList<TransactionBuilder> tailToHead = new LinkedList<>();

    public void append(List<TransactionBuilder> unfinishedTransactionsFromTailToHead) {
        for(TransactionBuilder unfinishedTransaction : unfinishedTransactionsFromTailToHead)
            append(unfinishedTransaction);
    }

    public void append(TransactionBuilder unfinishedTransaction) {
        tailToHead.add(unfinishedTransaction);
    }

    public Bundle build() {
        setFlags();
        Transaction head = buildTrunkLinkedChainAndReturnHead();
        return tailToHead.size() > 0 ? new Bundle(head) : null;
    }

    private Transaction buildTrunkLinkedChainAndReturnHead() {

        Transaction lastTransaction = null;
        for (int i = 0; i < tailToHead.size(); i++) {
            boolean isFirst = i == 0;
            TransactionBuilder unfinished = tailToHead.get(i);
            if(!isFirst)
                unfinished.trunkHash = lastTransaction.hash;
            Transaction currentTransaction = unfinished.build();
            currentTransaction.trunk = lastTransaction;
            lastTransaction = currentTransaction;
        }

        return lastTransaction;
    }

    private void setFlags() {
        for(TransactionBuilder unfinished : tailToHead) {
            unfinished.isBundleHead = false;
            unfinished.isBundleTail = false;
        }
        tailToHead.getFirst().isBundleTail = true;
        tailToHead.getLast().isBundleHead = true;
    }
}
