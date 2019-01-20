package org.iota.ict.model;

import com.iota.curl.IotaCurlHash;
import org.iota.ict.utils.Constants;
import org.iota.ict.utils.Trytes;

import java.math.BigInteger;
import java.net.DatagramPacket;

/**
 * Instances of this class are IOTA transactions which together form a tangle. Actually an IOTA transaction is no more
 * than a sequence of trytes. This class makes it possible to interpret these trytes and operate on them. A transaction's
 * tryte sequence consists of multiple tryte fields (e.g. {@link #address}, {@link #value}, {@link #nonce}, see {@link Transaction.Field}).
 * of static length. With this class, these fields can be easily accessed. {@link Transaction} objects are data  objects,
 * and their fields are not supposed to change after instantiation. To create custom transaction, one should use {@link TransactionBuilder#build()}.
 *
 * @see TransactionBuilder to create new instances.
 * @see Tangle as database for transactions during tuntime.
 * @see Bundle which is a structure consisting of multiple transactions.
 */
public class Transaction {

    public static final Transaction NULL_TRANSACTION = new Transaction();

    private String signatureFragments;
    private String extraDataDigest;
    private String address;
    private String bundleNonce;
    private String trunkHash, branchHash;
    private String tag;
    private String nonce;
    private String decodedSignatureFragments;
    private String essence;

    public final long issuanceTimestamp;
    public final long timelockLowerBound, timelockUpperBound;
    public final long attachmentTimestamp, attachmentTimestampLowerBound, attachmentTimestampUpperBound;

    public BigInteger value;
    public final String hash;

    public final boolean isBundleHead, isBundleTail;

    private final byte[] bytes;
    transient Transaction branch;
    transient Transaction trunk;

    /**
     * Creates the NULL transaction. All trits are 0. Requires separate constructor because
     * all trit flags are set to 0 thus making this transaction actually invalid.
     */
    private Transaction() {
        bytes = new byte[Constants.PACKET_SIZE_BYTES];
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

        hash = curlHash(trytes());
        isBundleHead = true;
        isBundleTail = true;

        branch = this;
        trunk = this;

        assert trytes().equals(Trytes.padRight("", Constants.TRANSACTION_SIZE_TRYTES+81));
    }

    private static String generateNullTrytes(Transaction.Field field) {
        return Trytes.padRight("", field.tryteLength);
    }

    Transaction(TransactionBuilder builder) {
        bytes = new byte[Constants.PACKET_SIZE_BYTES];
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

        String trytes = trytes();
        if(!Trytes.isTrytes(trytes))
            throw new IllegalArgumentException("Transaction contains non-tryte characters.");

        hash = curlHash(trytes);
        decodedSignatureFragments = Trytes.toAscii(signatureFragments);
        System.arraycopy(Trytes.toBytes(trytes), 0, bytes, 0, Constants.TRANSACTION_SIZE_BYTES);

        byte[] hashTrits = Trytes.toTrits(hash);
        isBundleHead = isFlagSet(hashTrits, Constants.HashFlags.BUNDLE_HEAD_FLAG);
        isBundleTail = isFlagSet(hashTrits, Constants.HashFlags.BUNDLE_TAIL_FLAG);
        assertMinWeightMagnitude(hashTrits);
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
        assert hashTrits.length == Field.TRUNK_HASH.tritLength;
        if (hashTrits[position] == 0)
            throw new InvalidTransactionFlagException(position);
        return hashTrits[position] == 1;
    }

    private static void assertMinWeightMagnitude(byte[] hashTrits) {
        if(!Constants.TESTING)
            for(int i = 0; i < Constants.MIN_WEIGHT_MAGNITUDE; i++)
                if(hashTrits[hashTrits.length-1-i] != 0)
                    throw new InvalidWeightException();
    }

    public Transaction(byte[] bytes) {
        assert bytes.length == Constants.PACKET_SIZE_BYTES;
        this.bytes = bytes;

        issuanceTimestamp = Trytes.toLong(decodeTryteField(Field.ISSUANCE_TIMESTAMP));
        timelockLowerBound = Trytes.toLong(decodeTryteField(Field.TIMELOCK_LOWER_BOUND));
        timelockUpperBound = Trytes.toLong(decodeTryteField(Field.TIMELOCK_UPPER_BOUND));

        attachmentTimestamp = Trytes.toLong(decodeTryteField(Field.ATTACHMENT_TIMESTAMP));
        attachmentTimestampLowerBound = Trytes.toLong(decodeTryteField(Field.ATTACHMENT_TIMESTAMP_LOWER_BOUND));
        attachmentTimestampUpperBound = Trytes.toLong(decodeTryteField(Field.ATTACHMENT_TIMESTAMP_UPPER_BOUND));
        value = Trytes.toNumber(decodeTryteField(Field.VALUE));

        hash = curlHash(bytes);

        byte[] hashTrits = Trytes.toTrits(hash);
        isBundleHead = isFlagSet(hashTrits, Constants.HashFlags.BUNDLE_HEAD_FLAG);
        isBundleTail = isFlagSet(hashTrits, Constants.HashFlags.BUNDLE_TAIL_FLAG);
        assertMinWeightMagnitude(hashTrits);
    }

    /**
     * @return Trytes of this transaction (length = {@link Constants#TRANSACTION_SIZE_TRYTES}).
     */
    private String trytes() {
        char[] trytes = new char[Constants.TRANSACTION_SIZE_TRYTES+81];
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
        System.arraycopy(Trytes.NULL_HASH.toCharArray(), 0, trytes, Constants.TRANSACTION_SIZE_TRYTES, 81); // TODO remove this constant part. it's just kept for now to keep it compatible with 0.4
        return new String(trytes);
    }

    protected String decodeTryteField(Field field) {
        return Trytes.fromBytes(bytes, field.byteOffset, field.byteLength);
    }

    private static String curlHash(byte[] bytes) {
        String trytes = Trytes.fromBytes(bytes, 0, Constants.TRANSACTION_SIZE_BYTES)+Trytes.NULL_HASH;
        return curlHash(trytes);
    }

    /**
     * @return Calculated hash of this transaction.
     */
    private static String curlHash(String trytes) {
        return IotaCurlHash.iotaCurlHash(trytes, Constants.TRANSACTION_SIZE_TRYTES+81, Constants.TESTING ? 9 : Constants.CURL_ROUNDS_TRANSACTION_HASH);
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

    public DatagramPacket toDatagramPacket(String requestHash) {
        byte[] requestHashBytes = Trytes.toBytes(requestHash);
        System.arraycopy(requestHashBytes, 0, bytes, Constants.TRANSACTION_SIZE_BYTES, requestHashBytes.length);
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

        ESSENCE = new Field(TRUNK_HASH.tritOffset - EXTRA_DATA_DIGEST.tritOffset, SIGNATURE_FRAGMENTS);


        public final int tritOffset, tritLength, tryteOffset, tryteLength, byteOffset, byteLength;

        private Field(int length, Field previousField) {
            assert length > 0;
            assert length % 3 == 0;
            this.tritLength = length;
            this.tritOffset = previousField == null ? 0 : previousField.tritOffset + previousField.tritLength;
            this.tryteOffset = tritOffset / 3;
            this.tryteLength = tritLength / 3;
            this.byteOffset = tryteOffset / 3 * 2;
            this.byteLength = tryteLength / 3 * 2;
        }
    }

    static class InvalidTransactionFlagException extends RuntimeException {
        InvalidTransactionFlagException(int flagIndex) {
            super("Flag defined in trit #" + flagIndex + " of transaction hash is invalid.");
        }
    }

    static class InvalidWeightException extends RuntimeException {
        InvalidWeightException() {
            super("Transaction does not satisfy minimum required weight magnitude = "+Constants.MIN_WEIGHT_MAGNITUDE+".");
        }
    }

    public String decodeBytesToTrytes() {
        return Trytes.fromBytes(bytes, 0, Constants.TRANSACTION_SIZE_BYTES);
    }

    public String address() { return address == null ? address = decodeTryteField(Field.ADDRESS) : address; }
    public String tag() { return tag == null ? tag = decodeTryteField(Field.TAG) : tag; }
    public String signatureFragments() { return signatureFragments == null ? signatureFragments = decodeTryteField(Field.SIGNATURE_FRAGMENTS) : signatureFragments; }
    public String extraDataDigest() { return extraDataDigest == null ? extraDataDigest = decodeTryteField(Field.EXTRA_DATA_DIGEST) : extraDataDigest; }
    public String bundleNonce() { return bundleNonce == null ? bundleNonce = decodeTryteField(Field.BUNDLE_NONCE) : bundleNonce; }
    public String trunkHash() { return trunkHash == null ? trunkHash = decodeTryteField(Field.TRUNK_HASH) : trunkHash; }
    public String branchHash() { return branchHash == null ? branchHash = decodeTryteField(Field.BRANCH_HASH) : branchHash; }
    public String nonce() { return nonce == null ? nonce = decodeTryteField(Field.NONCE) : nonce; }
    public String essence() { return essence == null ? essence = decodeTryteField(Field.ESSENCE) : essence; }

    public String decodedSignatureFragments() { return decodedSignatureFragments == null ? decodedSignatureFragments = Trytes.toAscii(signatureFragments()) : decodedSignatureFragments; }
}