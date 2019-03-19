package org.iota.ict.model.tangle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.iota.ict.Ict;
import org.iota.ict.IctInterface;
import org.iota.ict.model.transaction.Transaction;
import org.iota.ict.network.Neighbor;
import org.iota.ict.utils.properties.FinalProperties;
import org.iota.ict.utils.properties.PropertiesUser;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Instances of this class provide a database which stores {@link Transaction} objects during runtime and allows to find
 * them by their hash, address or tag. Each {@link Ict} uses a {@link Tangle} object to keep track of all received transactions.
 */
public class Tangle implements PropertiesUser {

    protected static final Logger LOGGER = LogManager.getLogger("Tangle");

    protected final IctInterface ict;
    protected final Map<String, TransactionLog> transactionsByHash = new ConcurrentHashMap<>();
    protected final Map<String, List<TransactionLog>> transactionsByAddress = new ConcurrentHashMap<>();
    protected final Map<String, List<TransactionLog>> transactionsByTag = new ConcurrentHashMap<>();
    protected final Map<String, List<Transaction>> waitingReferrersTransactionsByHash = new ConcurrentHashMap<>();

    public Tangle(IctInterface ict) {
        this.ict = Objects.requireNonNull(ict, "'ict' must not null");
        createTransactionLogIfAbsent(Transaction.NULL_TRANSACTION);
    }

    @Override
    public void updateProperties(FinalProperties properties) {
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
        Tangle.TransactionLog log = transactionsByHash.get(hash);
        return log!=null ? log.transaction : null;
    }

    public Set<Transaction> findTransactionsByAddress(String address) {
        Set<Transaction> transactions = new HashSet<>();
        List<Tangle.TransactionLog> txByAddress = transactionsByAddress.get(address);
        if (txByAddress!=null) {
            for (TransactionLog log : txByAddress)
                transactions.add(log.transaction);
        }
        return transactions;
    }

    public Set<Transaction> findTransactionsByTag(String tag) {
        Set<Transaction> transactions = new HashSet<>();
        List<Tangle.TransactionLog> txByTag = transactionsByTag.get(tag);
        if (txByTag!=null) {
            for (TransactionLog log : txByTag)
                transactions.add(log.transaction);
        }
        return transactions;
    }

    public void deleteTransaction(Transaction transaction) {
        transaction.setTrunk(null);
        transaction.setBranch(null);
        TransactionLog log = transactionsByHash.remove(transaction.hash);
        if (log != null) {
            log.removeFromSetMap(transactionsByTag, transaction.tag());
            log.removeFromSetMap(transactionsByAddress, transaction.address());
        }
    }

    private void buildEdges(Transaction transaction) {
        buildEdgesToReferringTransactions(transaction);
        buildEdgesToReferencedTransactions(transaction);
    }

    private void buildEdgesToReferringTransactions(Transaction referred) {
        if (waitingReferrersTransactionsByHash.containsKey(referred.hash)) {
            List<Transaction> waiters = waitingReferrersTransactionsByHash.get(referred.hash);
            for (Transaction waiter : waiters)
                buildEdgesToReferencedTransactions(waiter);
            waitingReferrersTransactionsByHash.remove(referred.hash);
        }
    }

    private void buildEdgesToReferencedTransactions(Transaction referrer) {
        referrer.setBranch(findTransactionOrPutOnWaitingList(referrer, referrer.branchHash()));
        referrer.setTrunk(findTransactionOrPutOnWaitingList(referrer, referrer.trunkHash()));
    }

    private Transaction findTransactionOrPutOnWaitingList(Transaction referrer, String branchOrTrunk) {
        Transaction transaction = findTransactionByHash(branchOrTrunk);
        if(transaction == null)
            addReferrerTransactionToWaitingList(referrer, branchOrTrunk);
        return transaction;
    }

    private void addReferrerTransactionToWaitingList(Transaction referrer, String transactionToWaitFor) {
        if (!waitingReferrersTransactionsByHash.containsKey(transactionToWaitFor)) {
            waitingReferrersTransactionsByHash.put(transactionToWaitFor, new CopyOnWriteArrayList<Transaction>());
            ict.request(transactionToWaitFor);
        }
        List<Transaction> waitingList = waitingReferrersTransactionsByHash.get(transactionToWaitFor);
        waitingList.add(referrer);
    }

    public int size() {
        return transactionsByHash.size();
    }

    public class TransactionLog {
        final Transaction transaction;
        public final Set<Neighbor> senders = new HashSet<>();
        public boolean wasSent;

        protected TransactionLog(Transaction transaction) {
            this.transaction = transaction;
            transactionsByHash.put(transaction.hash, this);
            insertIntoSetMap(transactionsByAddress, transaction.address());
            insertIntoSetMap(transactionsByTag, transaction.tag());

            // buildEdges() must be called after transactionsByHash.put() because first tx (NULL tx) is referencing itself
            buildEdges(transaction);
        }

        protected <K> void insertIntoSetMap(Map<K, List<TransactionLog>> map, K key) {
            if (!map.containsKey(key)) {
                map.put(key, new CopyOnWriteArrayList<TransactionLog>());
            }
            map.get(key).add(this);
        }

        protected <K> void removeFromSetMap(Map<K, List<TransactionLog>> map, K key) {
            if (map.containsKey(key)) {
                map.get(key).remove(this);
                if (map.get(key).size() == 0)
                    map.remove(key);
            }
        }
    }
}