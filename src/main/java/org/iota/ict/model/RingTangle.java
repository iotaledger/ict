package org.iota.ict.model;

import org.iota.ict.IctInterface;
import org.iota.ict.utils.properties.FinalProperties;
import org.iota.ict.utils.properties.PropertiesUser;

import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * This Tangle prunes transactions after reaching a certain size. It works similar to a ring memory (hence the name).
 * The transactions are pruned in order of their timestamp, always keeping the N ({@link #transactionCapacity}) most recent ones.
 * As an exception, the NULL transaction will never be pruned away.
 */
public class RingTangle extends Tangle implements PropertiesUser {

    private final PriorityBlockingQueue<Transaction> transactionsOrderedByTimestamp ;
    private long transactionCapacity;

    public RingTangle(IctInterface ict) {
        super(ict);
        transactionCapacity = ict.getProperties().tangleCapacity();
        transactionsOrderedByTimestamp = new PriorityBlockingQueue<>((int)Math.min(Integer.MAX_VALUE, transactionCapacity), TimestampComparator.INSTANCE);
    }

    @Override
    public void updateProperties(FinalProperties newProperties) {
        transactionCapacity = newProperties.tangleCapacity();
    }

    public TransactionLog createTransactionLogIfAbsent(Transaction transaction) {

        TransactionLog log = super.createTransactionLogIfAbsent(transaction);
        if (transactionsOrderedByTimestamp != null) {
            // == null only when calling the super constructor and adding NULL transaction
            // do not add NULL transaction to transactionsOrderedByTimestamp to prevent it from being pruned
            transactionsOrderedByTimestamp.put(transaction);
            while (transactionsOrderedByTimestamp.size() + 1 > transactionCapacity) { // +1 fpr NULL transaction
                deleteTransaction(transactionsOrderedByTimestamp.poll());
            }
            assert size() <= transactionCapacity;
        }
        return log;
    }

    @Override
    public void deleteTransaction(Transaction transaction) {

        TransactionLog log = transactionsByHash.remove(transaction.hash);
        if (log != null) {
            log.removeFromSetMap(transactionsByTag, transaction.tag);
            log.removeFromSetMap(transactionsByAddress, transaction.address);
            transactionsOrderedByTimestamp.remove(log);
        }
    }

    private static class TimestampComparator implements Comparator<Transaction> {

        static final TimestampComparator INSTANCE = new TimestampComparator();

        @Override
        public int compare(Transaction tl1, Transaction tl2) {
            int cmp = Long.compare(tl1.issuanceTimestamp, tl2.issuanceTimestamp);
            return cmp == 0 ? tl1.hash.compareTo(tl2.hash) : cmp;
        }

        @Override
        public boolean equals(Object o) {
            return super.equals(o);
        }
    }
}