package org.iota.ict.model;

import com.iota.curl.IotaCurlHash;
import org.iota.ict.utils.Constants;
import org.iota.ict.utils.Trytes;

import java.math.BigInteger;
import java.net.DatagramPacket;

public class Transaction {
    public final String signatureFragments;
    public final String extraDataDigest;
    public final String address;
    public final BigInteger value;
    public final long issuanceTimestamp;
    public final long timelockLowerBound, timelockUpperBound;
    public final String bundleNonce;
    public final String trunk, branch;
    public final String tag;
    public final long attachmentTimestamp, attachmentTimestampLowerBound, attachmentTimestampUpperBound;
    public final String nonce;
    public final String decodedSignatureFragments;

    public final String trytes;
    public final String requestHash;
    public final String hash;

    Transaction(TransactionBuilder builder) {
        signatureFragments = builder.signatureFragments;
        extraDataDigest = builder.extraDataDigest;
        address = builder.address;
        value = builder.value;
        issuanceTimestamp = builder.issuanceTimestamp;
        timelockLowerBound = builder.timelockLowerBound;
        timelockUpperBound = builder.timelockUpperBound;
        bundleNonce = builder.bundleNonce;
        trunk = builder.trunk;
        branch = builder.branch;
        tag = builder.tag;
        attachmentTimestamp = builder.attachmentTimestamp;
        attachmentTimestampLowerBound = builder.attachmentTimestampLowerBound;
        attachmentTimestampUpperBound = builder.attachmentTimestampUpperBound;
        nonce = builder.nonce;
        requestHash = builder.requestHash;

        trytes = trytes();
        hash = curlHash();
        decodedSignatureFragments = Trytes.toAscii(signatureFragments);

        // TODO validate transaction format
    }

    public Transaction(String trytes) {
        assert trytes.length() == Constants.TRANSACTION_SIZE / 3;
        signatureFragments = extractField(trytes, Field.SIGNATURE_FRAGMENTS);
        extraDataDigest = extractField(trytes, Field.EXTRA_DATA_DIGEST);
        address = extractField(trytes, Field.ADDRESS);
        value = Trytes.toNumber(extractField(trytes, Field.VALUE));
        issuanceTimestamp = Trytes.toLong(extractField(trytes, Field.ISSUANCE_TIMESTAMP));
        timelockLowerBound = Trytes.toLong(extractField(trytes, Field.TIMELOCK_LOWER_BOUND));
        timelockUpperBound = Trytes.toLong(extractField(trytes, Field.TIMELOCK_UPPER_BOUND));
        bundleNonce = extractField(trytes, Field.BUNDLE_NONCE);
        trunk = extractField(trytes, Field.TRUNK);
        branch = extractField(trytes, Field.BRANCH);
        tag = extractField(trytes, Field.TAG);
        attachmentTimestamp = Trytes.toLong(extractField(trytes, Field.ATTACHMENT_TIMESTAMP));
        attachmentTimestampLowerBound = Trytes.toLong(extractField(trytes, Field.ATTACHMENT_TIMESTAMP_LOWER_BOUND));
        attachmentTimestampUpperBound = Trytes.toLong(extractField(trytes, Field.ATTACHMENT_TIMESTAMP_UPPER_BOUND));
        nonce = extractField(trytes, Field.NONCE);
        requestHash = extractField(trytes, Field.REQUEST_HASH);

        this.trytes = trytes();
        assert this.trytes.equals(trytes);
        hash = curlHash();
        decodedSignatureFragments = Trytes.toAscii(signatureFragments);

        // TODO validate transaction format
    }

    private String trytes() {
        char[] trytes = new char[Constants.TRANSACTION_SIZE / 3];
        putField(trytes, Field.SIGNATURE_FRAGMENTS, signatureFragments);
        putField(trytes, Field.EXTRA_DATA_DIGEST, extraDataDigest);
        putField(trytes, Field.ADDRESS, address);
        putField(trytes, Field.VALUE, value);
        putField(trytes, Field.ISSUANCE_TIMESTAMP, issuanceTimestamp);
        putField(trytes, Field.TIMELOCK_LOWER_BOUND, timelockLowerBound);
        putField(trytes, Field.TIMELOCK_UPPER_BOUND, timelockUpperBound);
        putField(trytes, Field.BUNDLE_NONCE, bundleNonce);
        putField(trytes, Field.TRUNK, trunk);
        putField(trytes, Field.BRANCH, branch);
        putField(trytes, Field.TAG, tag);
        putField(trytes, Field.ATTACHMENT_TIMESTAMP, attachmentTimestamp);
        putField(trytes, Field.ATTACHMENT_TIMESTAMP_LOWER_BOUND, attachmentTimestampLowerBound);
        putField(trytes, Field.ATTACHMENT_TIMESTAMP_UPPER_BOUND, attachmentTimestampUpperBound);
        putField(trytes, Field.NONCE, nonce);
        putField(trytes, Field.REQUEST_HASH, requestHash);
        return new String(trytes);
    }

    private String curlHash() {
        return IotaCurlHash.iotaCurlHash(trytes, trytes.length(), 123);
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
        return transactionTrytes.substring(field.tryteOffset, field.tryteOffset + field.tryteLength);
    }

    public DatagramPacket toDatagramPacket() {
        return new DatagramPacket(trytes.getBytes(), trytes.getBytes().length);
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
                TRUNK = new Field(243, BUNDLE_NONCE),
                BRANCH = new Field(243, TRUNK),
                TAG = new Field(81, BRANCH),
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
}
