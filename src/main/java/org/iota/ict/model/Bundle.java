package org.iota.ict.model;

import com.iota.curl.IotaCurlHash;
import org.iota.ict.Ict;
import org.iota.ict.utils.Constants;

import java.util.ArrayList;
import java.util.List;

/**
 * This class allows to operate on the Bundle structure, a linear sequence of linked transactions. Note that anything
 * related to value transactions and signatures is not modelled by this class. Instead, this class is reduced to the
 * core Bundle structure.
 *
 * @see Transfer for value and signature related functionality.
 */
public class Bundle {
    private final ArrayList<Transaction> transactions = new ArrayList<>();
    private boolean complete, structureValid;
    private String hash;

    /**
     * Fetches the bundle from a bundle head transaction which points to the entire bundle.
     *
     * @param head Bundle head ({@link Transaction#isBundleHead} must be {@code true})
     */
    public Bundle(Transaction head) {
        if (!head.isBundleHead)
            throw new IllegalArgumentException("Transaction " + head.hash + " is not a bundle head.");
        transactions.add(head);
        build();
    }

    /**
     * Keeps adding transactions to the bundle and re-validates it until it is complete (either because all transactions
     * have been added or because the bundle structure is invalid).
     */
    public void build() {
        if (complete)
            return;

        Transaction fetchedLast = transactions.get(transactions.size() - 1);
        while (fetchedLast.trunk != null && !fetchedLast.isBundleTail && (!fetchedLast.isBundleHead || transactions.size() == 1)) {
            transactions.add(fetchedLast.trunk);
            fetchedLast = fetchedLast.trunk;
        }

        if (fetchedLast.isBundleTail)
            complete(true);

        if (!fetchedLast.isBundleTail && fetchedLast.isBundleHead && transactions.size() > 1)
            complete(false); // new bundle opened by head before current bundle closed by tail
    }

    private void complete(boolean structureValid) {
        this.structureValid = structureValid;
        hash = calcHash();
        complete = true;
    }

    /**
     * @param ict Ict to request transaction with.
     *            Completes the bundle if all tranactions are already linked together by trunk. Otherwise requests the next transactions
     *            that is still missing.
     */
    public void tryToComplete(Ict ict) {
        build();
        if (complete)
            return;
        Transaction fetchedLast = transactions.get(transactions.size() - 1);
        ict.request(fetchedLast.trunkHash);
    }

    /**
     * @return All transactions in this bundle in correct order from head to tail.
     * @throws IllegalStateException if queried before {@link #isComplete() is {@code true}}.
     */
    public List<Transaction> getTransactions() {
        assertCompleteAndStructureValid("return transactions");
        return new ArrayList<>(transactions);
    }

    /**
     * @return {@code true} if bundle is opened by head and closed by tail. {@code false} otherwise (not a bundle). <b>NOTE: Does not validate signature.</b>
     * @throws IllegalStateException if queried before {@link #isComplete() is {@code true}}.
     */
    public boolean isStructureValid() {
        if (!complete)
            throw new IllegalStateException("Bundle has not yet been fetched completely. Cannot validate structure.");
        return structureValid;
    }

    public String getHash() {
        assertCompleteAndStructureValid("calculate hash");
        return hash;
    }

    private String calcHash() {
        StringBuilder bundleEssence = new StringBuilder();
        for (Transaction transaction : transactions)
            bundleEssence.append(transaction.essence);
        return IotaCurlHash.iotaCurlHash(bundleEssence.toString(), bundleEssence.length(), Constants.CURL_ROUNDS_BUNDLE_HASH);
    }

    private void assertCompleteAndStructureValid(String action) {
        if (!complete)
            throw new IllegalStateException("Bundle has not yet been fetched completely yet. Cannot " + action + ".");
        if (!structureValid)
            throw new IllegalStateException("Bundle structure is invalid. Cannot " + action + ".");
    }

    /**
     * @return {@code true} if bundle has either been fetched completely from head to tail or if structure is invalid
     */
    public boolean isComplete() {
        return complete;
    }
}
