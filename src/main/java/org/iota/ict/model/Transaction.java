package org.iota.ict.model;

import com.iota.curl.IotaCurlHash;
import org.iota.ict.utils.Constants;
import org.iota.ict.utils.Trytes;

import java.io.Serializable;
import java.math.BigInteger;
import java.net.DatagramPacket;

public class Transaction implements Serializable {

    public static final Transaction NULL_TRANSACTION = new Transaction();

    public final String signatureFragments;
    public final String extraDataDigest;
    public final String address;
    public final BigInteger value;
    public final long issuanceTimestamp;
    public final long timelockLowerBound, timelockUpperBound;
    public final String bundleNonce;
    public final String trunkHash, branchHash;
    public final String tag;
    public final long attachmentTimestamp, attachmentTimestampLowerBound, attachmentTimestampUpperBound;
    public final String nonce;
    public final String decodedSignatureFragments;

    public final String trytes;
    public String requestHash;
    public final String hash;

    public final boolean isBundleHead, isBundleTail;

    Transaction branch;
    Transaction trunk;

    /**
     * Creates the NULL transaction. All trits are 0. Requires separate constructor because
     * all trit flags are set to 0 thus making this transaction actually invalid.
     */
    private Transaction() {
        signatureFragments = generateNullTrytes(Field.SIGNATURE_FRAGMENTS);
        extraDataDigest = generateNullTrytes(Field.EXTRA_DATA_DIGEST);
        address = generateNullTrytes(Field.ADDRESS);
        value = BigInteger.ZERO;
        issuanceTimestamp = 0;
        timelockLowerBound = 0;
        timelockUpperBound = 0;
        bundleNonce = generateNullTrytes(Field.BUNDLE_NONCE);
        trunkHash = generateNullTrytes(Field.TRUNK_HASH);
        branchHash = generateNullTrytes(Field.BRANCH_HASH);
        tag = generateNullTrytes(Field.TAG);
        attachmentTimestamp = 0;
        attachmentTimestampLowerBound = 0;
        attachmentTimestampUpperBound = 0;
        nonce = generateNullTrytes(Field.NONCE);
        decodedSignatureFragments = "";

        trytes = trytes();
        hash = curlHash();
        isBundleHead = true;
        isBundleTail = true;

        branch = this;
        trunk = this;

        assert trytes().matches("^[9]{" + Constants.TRANSACTION_SIZE_TRYTES + "}$");
    }

    private static String generateNullTrytes(Transaction.Field field) {
        return Trytes.padRight("", field.tryteLength);
    }

    Transaction(TransactionBuilder builder) {
        signatureFragments = builder.signatureFragments;
        extraDataDigest = builder.extraDataDigest;
        address = builder.address;
        value = builder.value;
        issuanceTimestamp = builder.issuanceTimestamp;
        timelockLowerBound = builder.timelockLowerBound;
        timelockUpperBound = builder.timelockUpperBound;
        bundleNonce = builder.bundleNonce;
        trunkHash = builder.trunkHash;
        branchHash = builder.branchHash;
        tag = builder.tag;
        attachmentTimestamp = builder.attachmentTimestamp;
        attachmentTimestampLowerBound = builder.attachmentTimestampLowerBound;
        attachmentTimestampUpperBound = builder.attachmentTimestampUpperBound;
        nonce = builder.nonce;
        requestHash = builder.requestHash;

        trytes = trytes();
        if (!Trytes.isTrytes(trytes))
            throw new IllegalArgumentException("at least one field contains non-tryte characters");

        hash = curlHash();
        decodedSignatureFragments = Trytes.toAscii(signatureFragments);

        byte[] hashTrits = Trytes.toTrits(hash);
        isBundleHead = isFlagSet(hashTrits, Constants.HashFlags.BUNDLE_HEAD_FLAG);
        isBundleTail = isFlagSet(hashTrits, Constants.HashFlags.BUNDLE_TAIL_FLAG);
    }

    /**
     * Determines the value of a flag in the hash trits.
     *
     * @param hashTrits The trit sequence of the transaction hash (length must be 243).
     * @param position  Position of the trit which defines this flag.
     * @return {@code true} if flag trit is 1, {@code false} if flag trit is -1
     * @throws InvalidTransactionFlagException if flag trit is 0.
     */
    private static boolean isFlagSet(byte[] hashTrits, int position) {
        assert hashTrits.length == Field.REQUEST_HASH.tritLength;
        if (hashTrits[position] == 0)
            throw new InvalidTransactionFlagException(position);
        return hashTrits[position] == 1;
    }

    /**
     * Creates a transaction object from its trytes.
     *
     * @param trytes Trytes containing all information describing this transaction. Must have length = {@link Constants#TRANSACTION_SIZE_TRYTES}.
     */
    public Transaction(String trytes) {
        assert trytes.length() == Constants.TRANSACTION_SIZE_TRYTES;
        signatureFragments = extractField(trytes, Field.SIGNATURE_FRAGMENTS);
        extraDataDigest = extractField(trytes, Field.EXTRA_DATA_DIGEST);
        address = extractField(trytes, Field.ADDRESS);
        value = Trytes.toNumber(extractField(trytes, Field.VALUE));
        issuanceTimestamp = Trytes.toLong(extractField(trytes, Field.ISSUANCE_TIMESTAMP));
        timelockLowerBound = Trytes.toLong(extractField(trytes, Field.TIMELOCK_LOWER_BOUND));
        timelockUpperBound = Trytes.toLong(extractField(trytes, Field.TIMELOCK_UPPER_BOUND));
        bundleNonce = extractField(trytes, Field.BUNDLE_NONCE);
        trunkHash = extractField(trytes, Field.TRUNK_HASH);
        branchHash = extractField(trytes, Field.BRANCH_HASH);
        tag = extractField(trytes, Field.TAG);
        attachmentTimestamp = Trytes.toLong(extractField(trytes, Field.ATTACHMENT_TIMESTAMP));
        attachmentTimestampLowerBound = Trytes.toLong(extractField(trytes, Field.ATTACHMENT_TIMESTAMP_LOWER_BOUND));
        attachmentTimestampUpperBound = Trytes.toLong(extractField(trytes, Field.ATTACHMENT_TIMESTAMP_UPPER_BOUND));
        nonce = extractField(trytes, Field.NONCE);
        requestHash = extractField(trytes, Field.REQUEST_HASH);

        this.trytes = trytes();
        assert trytes.startsWith(this.trytes.substring(0, Field.REQUEST_HASH.tryteOffset));
        hash = curlHash();
        decodedSignatureFragments = Trytes.toAscii(signatureFragments);

        byte[] hashTrits = Trytes.toTrits(hash);
        isBundleHead = isFlagSet(hashTrits, Constants.HashFlags.BUNDLE_HEAD_FLAG);
        isBundleTail = isFlagSet(hashTrits, Constants.HashFlags.BUNDLE_TAIL_FLAG);
    }

    /**
     * @return Trytes of this transaction (length = {@link Constants#TRANSACTION_SIZE_TRYTES}). {@link #requestHash} trytes are always NULL.
     */
    private String trytes() {
        char[] trytes = new char[Constants.TRANSACTION_SIZE_TRYTES];
        putField(trytes, Field.SIGNATURE_FRAGMENTS, signatureFragments);
        putField(trytes, Field.EXTRA_DATA_DIGEST, extraDataDigest);
        putField(trytes, Field.ADDRESS, address);
        putField(trytes, Field.VALUE, value);
        putField(trytes, Field.ISSUANCE_TIMESTAMP, issuanceTimestamp);
        putField(trytes, Field.TIMELOCK_LOWER_BOUND, timelockLowerBound);
        putField(trytes, Field.TIMELOCK_UPPER_BOUND, timelockUpperBound);
        putField(trytes, Field.BUNDLE_NONCE, bundleNonce);
        putField(trytes, Field.TRUNK_HASH, trunkHash);
        putField(trytes, Field.BRANCH_HASH, branchHash);
        putField(trytes, Field.TAG, tag);
        putField(trytes, Field.ATTACHMENT_TIMESTAMP, attachmentTimestamp);
        putField(trytes, Field.ATTACHMENT_TIMESTAMP_LOWER_BOUND, attachmentTimestampLowerBound);
        putField(trytes, Field.ATTACHMENT_TIMESTAMP_UPPER_BOUND, attachmentTimestampUpperBound);
        putField(trytes, Field.NONCE, nonce);
        putField(trytes, Field.REQUEST_HASH, Trytes.NULL_HASH);
        return new String(trytes);
    }

    /**
     * Requires {@link #trytes} to be set.
     *
     * @return Calculated hash of this transaction.
     */
    private String curlHash() {
        return IotaCurlHash.iotaCurlHash(trytes, trytes.length(), Constants.CURL_ROUNDS_TRANSACTION_HASH);
    }

    private static void putField(char[] target, Field field, long value) {
        putField(target, field, new BigInteger(value + ""));
    }

    private static void putField(char[] target, Field field, BigInteger bigInteger) {
        String trytes = Trytes.fromNumber(bigInteger, field.tryteLength);
        putField(target, field, trytes);
    }

    private static void putField(char[] target, Field field, String trytes) {
        assert trytes.length() == field.tryteLength;
        System.arraycopy(trytes.toCharArray(), 0, target, field.tryteOffset, field.tryteLength);
    }

    private static String extractField(String transactionTrytes, Field field) {
        String extracted = transactionTrytes.substring(field.tryteOffset, field.tryteOffset + field.tryteLength);
        if (!Trytes.isTrytes(extracted))
            throw new IllegalArgumentException("field starting at offset '" + field.tryteOffset + "' contains non tryte characters");
        return extracted;
    }

    public DatagramPacket toDatagramPacket() {
        String fullTrytes = trytes.substring(0, Field.REQUEST_HASH.tryteOffset) + requestHash;
        byte[] bytes = Trytes.toBytes(fullTrytes);
        return new DatagramPacket(bytes, bytes.length);
    }

    public Transaction getBranch() {
        return branch;
    }

    public Transaction getTrunk() {
        return trunk;
    }

    public static class Field {
        public static final Field SIGNATURE_FRAGMENTS = new Field(6561, null),
                EXTRA_DATA_DIGEST = new Field(243, SIGNATURE_FRAGMENTS),
                ADDRESS = new Field(243, EXTRA_DATA_DIGEST),
                VALUE = new Field(81, ADDRESS),
                ISSUANCE_TIMESTAMP = new Field(27, VALUE),
                TIMELOCK_LOWER_BOUND = new Field(27, ISSUANCE_TIMESTAMP),
                TIMELOCK_UPPER_BOUND = new Field(27, TIMELOCK_LOWER_BOUND),
                BUNDLE_NONCE = new Field(81, TIMELOCK_UPPER_BOUND),
                TRUNK_HASH = new Field(243, BUNDLE_NONCE),
                BRANCH_HASH = new Field(243, TRUNK_HASH),
                TAG = new Field(81, BRANCH_HASH),
                ATTACHMENT_TIMESTAMP = new Field(27, TAG),
                ATTACHMENT_TIMESTAMP_LOWER_BOUND = new Field(27, ATTACHMENT_TIMESTAMP),
                ATTACHMENT_TIMESTAMP_UPPER_BOUND = new Field(27, ATTACHMENT_TIMESTAMP_LOWER_BOUND),
                NONCE = new Field(81, ATTACHMENT_TIMESTAMP_UPPER_BOUND),
                REQUEST_HASH = new Field(243, NONCE);


        public final int tritOffset, tritLength, tryteOffset, tryteLength;

        private Field(int length, Field previousField) {
            assert length > 0;
            assert length % 3 == 0;
            this.tritLength = length;
            this.tritOffset = previousField == null ? 0 : previousField.tritOffset + previousField.tritLength;
            this.tryteOffset = tritOffset / 3;
            this.tryteLength = tritLength / 3;
        }
    }

    static class InvalidTransactionFlagException extends RuntimeException {
        InvalidTransactionFlagException(int flagIndex) {
            super("Flag defined in trit #" + flagIndex + " of transaction hash is invalid.");
        }
    }
}
