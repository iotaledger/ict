package org.iota.ict.model;

import java.math.BigInteger;

public class BalanceChangeBuilder {

    public String address;
    public BigInteger value;
    public final StringBuilder signatureOrMessage;

    public BalanceChangeBuilder(Transaction transaction) {
        this.address = transaction.address;
        this.value = transaction.value;
        this.signatureOrMessage = new StringBuilder(transaction.signatureFragments);
    }

    public BalanceChangeBuilder(String address, BigInteger value, String signatureOrMessage) {
        this.address = address;
        this.value = value;
        this.signatureOrMessage = new StringBuilder(signatureOrMessage);
    }

    public void append(Transaction transaction) {
        if(!address.equals(transaction.address))
            throw new IllegalArgumentException("cannot append transaction from different address");
        value = value.add(transaction.value);
        signatureOrMessage.append(transaction.signatureFragments);
    }

    public BalanceChange build() {
        return new BalanceChange(address, value, signatureOrMessage.toString());
    }
}
