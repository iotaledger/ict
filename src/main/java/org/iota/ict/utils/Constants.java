package org.iota.ict.utils;

import org.iota.ict.model.Transaction;

/**
 * Important constants which are not changed during runtime but might be changed during development or are used by
 * multiple classes are kept together here to make them easier to find and adjust.
 */
public class Constants {
    public static final String ICT_VERSION = "0.3-SNAPSHOT";
    public static final int MAX_NEIGHBOR_COUNT = 3;
    public static final int TRANSACTION_SIZE_TRITS = Transaction.Field.REQUEST_HASH.tritOffset + Transaction.Field.REQUEST_HASH.tritLength;
    public static final int TRANSACTION_SIZE_TRYTES = TRANSACTION_SIZE_TRITS / 3;
    public static final int TRANSACTION_SIZE_BYTES = TRANSACTION_SIZE_TRITS / 9 * 2;
    public static final int CURL_ROUNDS_TRANSACTION_HASH = 123;
    public static final int CURL_ROUNDS_BUNDLE_HASH = 123;

    public static boolean TESTING = true;

    /**
     * Specifies through which trit of the transaction hash each flag is defined.
     */
    public static final class HashFlags {
        public static final int BUNDLE_HEAD_FLAG = 1;
        public static final int BUNDLE_TAIL_FLAG = 2;
    }
}