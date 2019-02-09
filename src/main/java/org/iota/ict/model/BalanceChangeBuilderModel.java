package org.iota.ict.model;

import java.math.BigInteger;
import java.util.List;

public interface BalanceChangeBuilderModel {

    BigInteger getValue();

    String getAddress();

    List<TransactionBuilder> getBuildersFromTailToHead();

    boolean hasSignature();
}
