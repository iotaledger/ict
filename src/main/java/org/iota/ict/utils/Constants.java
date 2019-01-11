package org.iota.ict.utils;

import org.iota.ict.model.Transaction;

import java.io.File;
import java.nio.file.Path;

/**
 * Important constants which are not changed during runtime but might be changed during development or are used by
 * multiple classes are kept together here to make them easier to find and adjust.
 */
public class Constants {

    public static final Path WORKING_DIRECTORY = (new File("./")).toPath();;

    public static final String ICT_VERSION = "0.4-SNAPSHOT";
    public static final String ICT_REPOSITORY = "iotaledger/ict";
    public static final String GITHUB_URL = "https://github.com/";
    public static final String DEFAULT_PROPERTY_FILE_PATH = "ict.cfg";

    public static final int MAX_NEIGHBOR_COUNT = 3;
    public static final int TRANSACTION_SIZE_TRITS = Transaction.Field.REQUEST_HASH.tritOffset + Transaction.Field.REQUEST_HASH.tritLength;
    public static final int TRANSACTION_SIZE_TRYTES = TRANSACTION_SIZE_TRITS / 3;
    public static final int TRANSACTION_SIZE_BYTES = TRANSACTION_SIZE_TRITS / 9 * 2;
    public static final int CURL_ROUNDS_TRANSACTION_HASH = 123;
    public static final int CURL_ROUNDS_BUNDLE_HASH = 123;
    public static final long TIMESTAMP_DIFFERENCE_TOLERANCE_IN_MILLIS = 20000;

    public static boolean TESTING = true;

    /**
     * Specifies through which trit of the transaction hash each flag is defined.
     */
    public static final class HashFlags {
        public static final int BUNDLE_HEAD_FLAG = 1;
        public static final int BUNDLE_TAIL_FLAG = 2;
    }
}