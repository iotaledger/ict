package org.iota.ict.model;

import com.iota.curl.IotaCurlHash;
import org.iota.ict.Ict;
import org.iota.ict.utils.Constants;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * This class allows to operate on the bundle structure, a linear sequence of linked {@link Transaction} objects.
 * Note that anything related to value transactions and signatures is not modelled by this class but by {@link Transfer}.
 * Instead, this classes scope is reduced to the core bundle structure, regardless of their content and interpretation.
 * <p>
 * Since it is not guaranteed that all transactions of a bundle become available at the same time, a bundle is not always
 * complete after instantiation. Whether it is can be checked with {@link #isComplete()}. To fetch missing parts of a bundle,
 * unknown transactions must be requested from neighbors - which can be done via {@link #tryToComplete(Ict)}.
 * <p>
 * Each transaction can be a bundle head or not (see {@link Transaction#isBundleHead}). The same applies to being a bundle
 * tail (see {@link Transaction#isBundleTail}). A bundle must always start with a bundle head and must end with a bundle
 * tail. Each inner transaction must be neither. If this principle is violated, the bundle structure is considered invalid.
 * This can be queried with {@link #isStructureValid()}.
 *
 * @see Transfer for value and signature related functionality.
 * @see BundleBuilder as a tool to create a new bundles.
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

    public Transaction getHead() {
        return transactions.get(0);
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
        ict.request(fetchedLast.trunkHash());
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
        StringBuilder concat = new StringBuilder();

        BalanceChange currentInput = null;
        for (Transaction transaction : transactions) {
            currentInput = determineInputOfTransaction(currentInput, transaction);
            boolean transactionIsOutput = currentInput == null;
            if (transactionIsOutput) {
                String hashOfMessage = IotaCurlHash.iotaCurlHash(transaction.signatureFragments(), transaction.signatureFragments().length(), Constants.CURL_ROUNDS_BUNDLE_HASH);
                concat.append(hashOfMessage);
            }
            concat.append(transaction.essence());
        }

        return IotaCurlHash.iotaCurlHash(concat.toString(), concat.length(), Constants.CURL_ROUNDS_BUNDLE_HASH);
    }

    private static BalanceChange determineInputOfTransaction(BalanceChange currentInput, Transaction transaction) {
        if (currentInput != null && (transaction.value.compareTo(BigInteger.ZERO) != 0 || !currentInput.address.equals(transaction.address())))
            currentInput = null;
        if (currentInput == null && transaction.value.compareTo(BigInteger.ZERO) < 0)
            currentInput = new BalanceChange(transaction.address(), transaction.value, "");
        return currentInput;

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
