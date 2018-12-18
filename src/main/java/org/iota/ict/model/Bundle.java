package org.iota.ict.model;

import org.iota.ict.Ict;

import java.util.ArrayList;
import java.util.List;

public class Bundle {
    private final ArrayList<Transaction> fragments = new ArrayList<>();
    private boolean complete, structureValid;

    /**
     * Fetches the bundle from a bundle head transaction which points to the entire bundle.
     *
     * @param head Bundle head ({@link Transaction#isBundleHead} must be {@code true})
     */
    public Bundle(Transaction head) {
        if (!head.isBundleHead)
            throw new IllegalArgumentException("Transaction " + head.hash + " is not a bundle head.");
        fragments.add(head);
        build();
    }

    /**
     * Keeps adding transactions to the bundle and re-validates it until it is complete (either because all transactions
     * have been added or because the bundle structure is invalid).
     */
    public void build() {
        if (complete)
            return;

        Transaction fetchedLast = fragments.get(fragments.size() - 1);
        while (fetchedLast.trunk != null && !fetchedLast.isBundleTail && (!fetchedLast.isBundleHead || fragments.size() == 1)) {
            fragments.add(fetchedLast.trunk);
            fetchedLast = fetchedLast.trunk;
        }

        if (fetchedLast.isBundleTail) {
            structureValid = true;
            complete = true;
        }

        if (!fetchedLast.isBundleTail && fetchedLast.isBundleHead && fragments.size() > 1) {
            // new bundle opened by head before current bundle closed by tail
            structureValid = false;
            complete = true;
        }
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
        Transaction fetchedLast = fragments.get(fragments.size() - 1);
        ict.request(fetchedLast.trunkHash);
    }

    /**
     * @return All transactions in this bundle in correct order from head to tail.
     * @throws IllegalStateException if queried before {@link #isComplete() is {@code true}}.
     */
    public List<Transaction> getTransactions() {
        if (!complete)
            throw new IllegalStateException("Bundle has not yet been fetched completely. Cannot validate structure.");
        return new ArrayList<>(fragments);
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

    /**
     * @return {@code true} if bundle has either been fetched completely from head to tail or if structure is invalid
     */
    public boolean isComplete() {
        return complete;
    }
}
