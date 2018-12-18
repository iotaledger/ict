package org.iota.ict.model;

import org.iota.ict.utils.Trytes;

import java.math.BigInteger;

public class TransactionBuilder {
    public String signatureFragments = generateNullTrytes(Transaction.Field.SIGNATURE_FRAGMENTS);
    public String extraDataDigest = generateNullTrytes(Transaction.Field.EXTRA_DATA_DIGEST);
    public String address = generateNullTrytes(Transaction.Field.ADDRESS);
    public BigInteger value = BigInteger.ZERO;
    public long issuanceTimestamp = System.currentTimeMillis();
    public long timelockLowerBound = 0, timelockUpperBound = 0;
    public String bundleNonce = generateNullTrytes(Transaction.Field.BUNDLE_NONCE);
    public String trunkHash = generateNullTrytes(Transaction.Field.TRUNK_HASH);
    public String branchHash = generateNullTrytes(Transaction.Field.BRANCH_HASH);
    public String tag = generateNullTrytes(Transaction.Field.TAG);
    public long attachmentTimestamp = System.currentTimeMillis(), attachmentTimestampLowerBound = 0, attachmentTimestampUpperBound = 0;
    String nonce = Trytes.randomSequenceOfLength(Transaction.Field.NONCE.tryteLength);
    public String requestHash = generateNullTrytes(Transaction.Field.REQUEST_HASH);
    public boolean isBundleHead = true;
    public boolean isBundleTail = true;

    public void asciiMessage(String asciiMessage) {
        signatureFragments = Trytes.padRight(Trytes.fromAscii(asciiMessage), Transaction.Field.SIGNATURE_FRAGMENTS.tryteLength);
    }

    private static String generateNullTrytes(Transaction.Field field) {
        return Trytes.padRight("", field.tryteLength);
    }

    public Transaction build() {
        // try different nonces to find transaction which satisfies required flagging by doing proof-of-work
        Transaction transaction = null;
        do {
            nonce = Trytes.randomSequenceOfLength(Transaction.Field.NONCE.tryteLength);
            try {
                transaction = new Transaction(this);
            } catch (Transaction.InvalidTransactionFlagException e) {
                // illegal flags, try next nonce
            }
        }
        while (transaction == null || transaction.isBundleTail != isBundleTail || transaction.isBundleHead != isBundleHead);
        return transaction;
    }
}
