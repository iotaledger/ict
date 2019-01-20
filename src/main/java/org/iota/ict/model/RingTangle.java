package org.iota.ict.model;

import org.iota.ict.IctInterface;
import org.iota.ict.utils.properties.FinalProperties;
import org.iota.ict.utils.properties.PropertiesUser;

import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * This Tangle prunes transactions after reaching a certain size. It works similar to a ring memory (hence the name).
 * The transactions are pruned in order of their timestamp, always keeping the N ({@link #capacity}) most recent ones.
 * As an exception, the NULL transaction will never be pruned away.
 */
public class RingTangle extends Tangle implements PropertiesUser {

    protected final PriorityBlockingQueue<Transaction> transactionsOrderedByTimestamp ;
    protected long capacity;
    protected double maxHeapSize;

    protected double capacityFactor = 1.0;
    protected long lastCapacityFactorChangeTimestamp = 0;

    public RingTangle(IctInterface ict) {
        super(ict);
        capacity = ict.getProperties().tangleCapacity();
        maxHeapSize = ict.getProperties().maxHeapSize();
        transactionsOrderedByTimestamp = new PriorityBlockingQueue<>((int)Math.min(Integer.MAX_VALUE, capacity), TimestampComparator.INSTANCE);
    }

    @Override
    public void updateProperties(FinalProperties newProperties) {
        capacity = newProperties.tangleCapacity();
        maxHeapSize = newProperties.maxHeapSize();
    }

    public TransactionLog createTransactionLogIfAbsent(Transaction transaction) {

        TransactionLog log = super.createTransactionLogIfAbsent(transaction);
        if (transactionsOrderedByTimestamp != null && !transactionsOrderedByTimestamp.contains(log.transaction)) {
            // == null only when calling the super constructor and adding NULL transaction
            // do not add NULL transaction to transactionsOrderedByTimestamp to prevent it from being pruned
            transactionsOrderedByTimestamp.put(transaction);
            while (transactionsOrderedByTimestamp.size() + 1 > capacity * capacityFactor) { // +1 fpr NULL transaction
                deleteTransaction(transactionsOrderedByTimestamp.poll());
            }
            assert size() <= capacity;
            adjustTangleCapacityFactor();
        }

        return log;
    }

    protected void adjustTangleCapacityFactor() {
        if(System.currentTimeMillis() - lastCapacityFactorChangeTimestamp > 60000) {
            double availableToUsedMemoryRatio = (Runtime.getRuntime().maxMemory() * maxHeapSize) / Runtime.getRuntime().totalMemory();
            double changeFactor =  Math.max(0.7, Math.min(1.4, availableToUsedMemoryRatio));
            double capacityFactorBefore = capacityFactor;
            capacityFactor = Math.min(1, Math.max(0.01, capacityFactor * changeFactor));
            lastCapacityFactorChangeTimestamp = System.currentTimeMillis();

            boolean majorCapacityFactorChange = capacityFactorBefore < 0.8 * capacityFactor || capacityFactorBefore > 1.2 * capacityFactor;
            if(majorCapacityFactorChange)
                LOGGER.info("Adjusting effective tangle_capacity to ~"+(int)Math.round(capacity * capacityFactor)+" transactions based on max_heap_size.");

            if(capacityFactor != capacityFactorBefore)
                LOGGER.debug("Adjusting effective tangle_capacity to ~"+(int)Math.round(capacity * capacityFactor)+" transactions based on max_heap_size.");
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