package org.iota.ict.model.bc;

import org.iota.ict.model.transaction.TransactionBuilder;

import java.util.List;

public interface BalanceChangeBuilderInterface extends BalanceChangeInterface {

    String getEssence();

    List<TransactionBuilder> getBuildersFromTailToHead();
}
