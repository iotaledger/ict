package org.iota.ict.model;

import org.iota.ict.Ict;
import org.iota.ict.network.Neighbor;
import org.iota.ict.utils.Constants;
import org.iota.ict.utils.Trytes;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Instances of this class provide a database which stores {@link Transaction} objects during runtime and allows to find
 * them by their hash, address or tag. Each {@link Ict} uses a {@link Tangle} object to keep track of all received transactions.
 */
public class Tangle {
    protected final Ict ict;
    protected final Map<String, TransactionLog> transactionsByHash = new ConcurrentHashMap<>();
    protected final Map<String, Set<TransactionLog>> transactionsByAddress = new ConcurrentHashMap<>();
    protected final Map<String, Set<TransactionLog>> transactionsByTag = new ConcurrentHashMap<>();
    protected final Map<String, Set<Transaction>> waitingReferrersTransactionsByHash = new ConcurrentHashMap<>();

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

    public Set<Transaction> findTransactionsByAddress(String address) {
        Set<Transaction> transactions = new HashSet<>();
        if(transactionsByAddress.containsKey(address)) {
            for(TransactionLog log : transactionsByAddress.get(address))
                transactions.add(log.transaction);
        }
        return transactions;
    }

    public Set<Transaction> findTransactionsByTag(String tag) {
        Set<Transaction> transactions = new HashSet<>();
        if(transactionsByTag.containsKey(tag)) {
            for(TransactionLog log : transactionsByTag.get(tag))
                transactions.add(log.transaction);
        }
        return transactions;
    }

    public void deleteTransaction(Transaction transaction) {
        TransactionLog log = transactionsByHash.remove(transaction.hash);
        if(log != null) {
            log.removeFromSetMap(transactionsByTag, transaction.tag);
            log.removeFromSetMap(transactionsByAddress, transaction.address);
        }
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
        final Transaction transaction;
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

        protected  <K> void removeFromSetMap(Map<K, Set<TransactionLog>> map, K key) {
            if (map.containsKey(key)) {
                map.get(key).remove(this);
                if(map.get(key).size() == 0)
                    map.remove(key);
            }
        }
    }
}