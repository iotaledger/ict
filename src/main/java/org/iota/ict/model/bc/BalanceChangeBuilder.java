package org.iota.ict.model.bc;

import org.iota.ict.model.transaction.TransactionBuilder;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

public abstract class BalanceChangeBuilder implements BalanceChangeBuilderInterface {

    private final BigInteger value;
    private final String address;
    protected final TransactionBuilder[] buildersFromTailToHead;

    public BalanceChangeBuilder(String address, BigInteger value, int fragments) {
        if(address == null)
            throw new NullPointerException("address");
        if(value == null)
            throw new NullPointerException("value");
        if(fragments <= 0)
            throw new IllegalArgumentException("fragments must be positive");
        this.value = value;
        this.address = address;
        this.buildersFromTailToHead = createTransactionBuildersFromTailToHead(fragments);
    }
    private TransactionBuilder[] createTransactionBuildersFromTailToHead(int fragments) {
        TransactionBuilder[] builders = new TransactionBuilder[fragments];
        for(int i = 0; i < fragments; i++) {
            TransactionBuilder builder = new TransactionBuilder();
            builder.address = address;
            builder.value = i == 0 ? value : BigInteger.ZERO;
            builders[i] =  builder;
        }
        return builders;
    }

    public List<TransactionBuilder> getBuildersFromTailToHead() {
        return Arrays.asList(buildersFromTailToHead);
    }

    public BigInteger getValue() {
        return value;
    }

    @Override
    public String getAddress() {
        return address;
    }
}
