package org.iota.ict.utils;

import org.iota.ict.model.Transaction;

public class Constants {
    public static final int MAX_NEIGHBOR_COUNT = 3;
    public static final int TRANSACTION_SIZE = Transaction.Field.REQUEST_HASH.tritOffset + Transaction.Field.REQUEST_HASH.tritLength;
    public static final int PACKET_SIZE = TRANSACTION_SIZE / 3;
    public static final int CURL_ROUNDS_TRANSACTION_HASH = 81;
}