package org.iota.ict.model;

import org.iota.ict.Ict;
import org.iota.ict.network.Neighbor;
import org.iota.ict.utils.Constants;
import org.iota.ict.utils.Trytes;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Tangle {
    private final Ict ict;
    private final Map<String, TransactionLog> transactionsByHash = new ConcurrentHashMap<>();
    private final Map<String, Set<TransactionLog>> transactionsByAddress = new ConcurrentHashMap<>();
    private final Map<String, Set<TransactionLog>> transactionsByTag = new ConcurrentHashMap<>();
    private final Map<String, Set<Transaction>> waitingReferrersTransactionsByHash = new ConcurrentHashMap<>();

    public Tangle(Ict ict) {
        this.ict = ict;
        createTransactionLogIfAbsent(Transaction.NULL_TRANSACTION);
    }

    public TransactionLog createTransactionLogIfAbsent(Transaction transaction) {
        TransactionLog log = findTransactionLog(transaction);
        if (log == null)
            log = new TransactionLog(transaction);
        return log;
    }

    public TransactionLog findTransactionLog(Transaction transaction) {
        return transactionsByHash.get(transaction.hash);
    }

    public Transaction findTransactionByHash(String hash) {
        return transactionsByHash.containsKey(hash) ? transactionsByHash.get(hash).transaction : null;
    }

    public void deleteTransaction(Transaction transaction) {
        transactionsByHash.remove(transaction.hash);
        transactionsByAddress.remove(transaction.address);
        transactionsByTag.remove(transaction.tag);
    }

    private void buildEdges(Transaction transaction) {
        buildEdgesToReferringTransactions(transaction);
        buildEdgesToReferencedTransactions(transaction);
    }

    private void buildEdgesToReferringTransactions(Transaction referred) {
        if (waitingReferrersTransactionsByHash.containsKey(referred.hash)) {
            Set<Transaction> waiters = waitingReferrersTransactionsByHash.get(referred.hash);
            for (Transaction waiter : waiters)
                buildEdgesToReferencedTransactions(waiter);
            waitingReferrersTransactionsByHash.remove(referred.hash);
        }
    }

    private void buildEdgesToReferencedTransactions(Transaction referrer) {

        referrer.branch = findTransactionByHash(referrer.branchHash);
        if (referrer.branch == null)
            addReferrerTransactionToWaitingList(referrer, referrer.branchHash);

        referrer.trunk = findTransactionByHash(referrer.trunkHash);
        if (referrer.trunk == null)
            addReferrerTransactionToWaitingList(referrer, referrer.trunkHash);
    }

    private void addReferrerTransactionToWaitingList(Transaction referrer, String transactionToWaitFor) {
        if (!waitingReferrersTransactionsByHash.containsKey(transactionToWaitFor)) {
            waitingReferrersTransactionsByHash.put(transactionToWaitFor, new HashSet<Transaction>());
            ict.request(transactionToWaitFor);
        }
        Set<Transaction> waitingList = waitingReferrersTransactionsByHash.get(transactionToWaitFor);
        waitingList.add(referrer);
    }

    public int size() {
        return transactionsByHash.size();
    }

    public class TransactionLog {
        private final Transaction transaction;
        public final Set<Neighbor> senders = new HashSet<>();
        public boolean sent;

        private TransactionLog(Transaction transaction) {
            this.transaction = transaction;
            transactionsByHash.put(transaction.hash, this);
            insertIntoSetMap(transactionsByAddress, transaction.address);
            insertIntoSetMap(transactionsByTag, transaction.tag);

            // buildEdges() must be called after transactionsByHash.put() because first tx (NULL tx) is referencing itself
            buildEdges(transaction);
        }

        private <K> void insertIntoSetMap(Map<K, Set<TransactionLog>> map, K key) {
            if (!map.containsKey(key)) {
                map.put(key, new HashSet<TransactionLog>());
            }
            map.get(key).add(this);
        }
    }
}