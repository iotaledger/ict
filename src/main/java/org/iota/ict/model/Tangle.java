package org.iota.ict.model;

import org.iota.ict.network.Neighbor;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Tangle {
    private final Map<String, TransactionLog> transactionsByHash = new ConcurrentHashMap<>();
    private final Map<String, Set<TransactionLog>> transactionsByAddress = new ConcurrentHashMap<>();
    private final Map<String, Set<TransactionLog>> transactionsByTag = new ConcurrentHashMap<>();

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

    public class TransactionLog {
        private final Transaction transaction;
        public final Set<Neighbor> senders = new HashSet<>();

        private TransactionLog(Transaction transaction) {
            this.transaction = transaction;
            transactionsByHash.put(transaction.hash, this);
            insertIntoSetMap(transactionsByAddress, transaction.address);
            insertIntoSetMap(transactionsByTag, transaction.tag);
        }

        private <K> void insertIntoSetMap(Map<K, Set<TransactionLog>> map, K key) {
            if (!map.containsKey(key)) {
                map.put(key, new HashSet<TransactionLog>());
            }
            map.get(key).add(this);
        }
    }
}