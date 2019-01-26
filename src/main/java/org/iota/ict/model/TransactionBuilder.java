package org.iota.ict.model;

import org.iota.ict.utils.Trytes;

import java.math.BigInteger;

/**
 * Since {@link Transaction} objects are final and their fields cannot be modified during runtime, the {@link TransactionBuilder}
 * can be used to define transaction fields before the transaction is created. This class is intended to create <b>new</b> transactions.
 */
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
    public boolean isBundleHead = true;
    public boolean isBundleTail = true;

    public void asciiMessage(String asciiMessage) {
        signatureFragments = Trytes.padRight(Trytes.fromAscii(asciiMessage), Transaction.Field.SIGNATURE_FRAGMENTS.tryteLength);
    }

    private static String generateNullTrytes(Transaction.Field field) {
        return Trytes.padRight("", field.tryteLength);
    }

    public Transaction buildWhileUpdatingTimestamp() {
        return doPow(true);
    }

    public Transaction build() {
        return doPow(false);
    }

    private Transaction doPow(boolean updateTimestamp) {
        // try different nonces to find transaction which satisfies required flagging by doing proof-of-work
        Transaction transaction = null;
        do {
            if (updateTimestamp)
                issuanceTimestamp = System.currentTimeMillis();
            nonce = Trytes.randomSequenceOfLength(Transaction.Field.NONCE.tryteLength);
            try {
                transaction = new Transaction(this);
            } catch (Transaction.InvalidTransactionFlagException | Transaction.InvalidWeightException e) {
                // illegal flags, try next nonce
            }
        }
        while (transaction == null || transaction.isBundleTail != isBundleTail || transaction.isBundleHead != isBundleHead);
        return transaction;
    }

    public String getEssence() {
        String essence = extraDataDigest;
        essence += address;
        essence += Trytes.fromNumber(value, Transaction.Field.VALUE.tryteLength);
        essence += Trytes.fromNumber(BigInteger.valueOf(issuanceTimestamp), Transaction.Field.ISSUANCE_TIMESTAMP.tryteLength);
        essence += Trytes.fromNumber(BigInteger.valueOf(timelockLowerBound), Transaction.Field.TIMELOCK_LOWER_BOUND.tryteLength);
        essence += Trytes.fromNumber(BigInteger.valueOf(timelockUpperBound), Transaction.Field.TIMELOCK_UPPER_BOUND.tryteLength);
        essence += bundleNonce;
        assert essence.length() == Transaction.Field.ESSENCE.tryteLength;
        return essence;
    }
}
