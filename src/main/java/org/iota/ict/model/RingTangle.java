package org.iota.ict.model;

import org.iota.ict.Ict;

import java.util.*;

/**
 * This Tangle prunes transactions after reaching a certain size. It works similar to a ring memory (hence the name).
 * The transactions are pruned in order of their timestamp, always keeping the N ({@link #transactionCapacity}) most recent ones.
 * As an exception, the NULL transaction will never be pruned away.
 * */
public class RingTangle extends Tangle {

    private final List<TransactionLog> transactionsOrderedByTimestamp = new ArrayList<>();
    private long transactionCapacity;

    public RingTangle(Ict ict, long transactionCapacity) {
        super(ict);
        this.transactionCapacity = transactionCapacity;
    }

    public TransactionLog createTransactionLogIfAbsent(Transaction transaction) {

        TransactionLog log = super.createTransactionLogIfAbsent(transaction);
        if(transactionsOrderedByTimestamp != null) {
            // == null only when calling the super constructor and adding NULL transaction
            // do not add NULL transaction to transactionsOrderedByTimestamp to prevent it from being pruned
            transactionsOrderedByTimestamp.add(log);
           if (transactionsOrderedByTimestamp.size()+1 > transactionCapacity) { // +1 fpr NULL transaction
               Collections.sort(transactionsOrderedByTimestamp, TimestampComparator.INSTANCE);
               deleteTransaction(transactionsOrderedByTimestamp.get(0).transaction);
           }
            assert size() <= transactionCapacity;
       }
        return log;
    }

    @Override
    public void deleteTransaction(Transaction transaction) {

        TransactionLog log = transactionsByHash.remove(transaction.hash);
        if(log != null) {
            log.removeFromSetMap(transactionsByTag, transaction.tag);
            log.removeFromSetMap(transactionsByAddress, transaction.address);
            transactionsOrderedByTimestamp.remove(log);
        }
    }

    private static class TimestampComparator implements Comparator<Tangle.TransactionLog> {

        static final TimestampComparator INSTANCE = new TimestampComparator();

        @Override
        public int compare(Tangle.TransactionLog tl1, Tangle.TransactionLog tl2) {
            int cmp = Long.compare(tl1.transaction.issuanceTimestamp, tl2.transaction.issuanceTimestamp);
            return cmp == 0 ? (tl1.equals(tl2) ? 0 : -1) : cmp;
        }

        @Override
        public boolean equals(Object o) {
            return super.equals(o);
        }
    }
}