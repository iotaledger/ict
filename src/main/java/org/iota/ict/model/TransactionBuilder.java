package org.iota.ict.model;

import org.iota.ict.utils.Trytes;

import java.math.BigInteger;

public class TransactionBuilder {
    public String signatureFragments = generateDummyTrytes(Transaction.Field.SIGNATURE_FRAGMENTS);
    public String extraDataDigest = generateDummyTrytes(Transaction.Field.EXTRA_DATA_DIGEST);
    public String address = generateDummyTrytes(Transaction.Field.ADDRESS);
    public BigInteger value = BigInteger.ZERO;
    public long issuanceTimestamp = System.currentTimeMillis();
    public long timelockLowerBound = 0, timelockUpperBound = 0;
    public String bundleNonce = generateDummyTrytes(Transaction.Field.BUNDLE_NONCE);
    public String trunkHash = generateDummyTrytes(Transaction.Field.TRUNK_HASH);
    public String branchHash = generateDummyTrytes(Transaction.Field.BRANCH_HASH);
    public String tag = generateDummyTrytes(Transaction.Field.TAG);
    public long attachmentTimestamp = System.currentTimeMillis(), attachmentTimestampLowerBound = 0, attachmentTimestampUpperBound = 0;
    public String nonce = Trytes.randomSequenceOfLength(Transaction.Field.NONCE.tryteLength);
    public String requestHash = generateDummyTrytes(Transaction.Field.REQUEST_HASH);

    public void asciiMessage(String asciiMessage) {
        signatureFragments = Trytes.padRight(Trytes.fromAscii(asciiMessage), Transaction.Field.SIGNATURE_FRAGMENTS.tryteLength);
    }

    private static String generateDummyTrytes(Transaction.Field field) {
        return Trytes.padRight("", field.tryteLength);
    }

    public Transaction build() {
        return new Transaction(this);
    }
}
