package org.iota.ict.ixi;

import org.iota.ict.Ict;
import org.iota.ict.network.event.GossipListener;
import org.iota.ict.model.Transaction;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public abstract class IXI {
    private final Ict ict;

    IXI(Ict ict) {
        this.ict = ict;
    }

    List<Transaction> getTransactions(Collection<String> hashes) {
        List<Transaction> transactions = new LinkedList<>();
        for (String hash : hashes) {
            Transaction transaction = ict.getTangle().findTransactionByHash(hash);
            if (transaction != null)
                transactions.add(transaction);
        }
        return transactions;
    }

    void putTransactions(Collection<Transaction> transactions) {
        for (Transaction transaction : transactions)
            ict.submit(transaction);
    }

    void addGossipListener(GossipListener listener) {
        ict.addGossipListener(listener);
    }

    /*
    void dumpTransactions(Collection<Transaction> transactions) throws IllegalAccessException
    {
        throw new IllegalAccessException("Deleting transactions is disabled until there is evidence that it might be useful. Please collaborate in case you need it.");
    }
    */
}
