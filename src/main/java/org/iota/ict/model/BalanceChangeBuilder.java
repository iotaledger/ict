package org.iota.ict.model;

import org.iota.ict.utils.Trytes;

import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;

/**
 * The {@link BalanceChangeBuilder} makes it possible to accumulate transactions which are part of the same {@link BalanceChange}
 * via {@link #append(Transaction)} or as container of {@link TransactionBuilder} (stored in {@link #buildersFromTailToHead})
 * during the creation of a new {@link Transfer}.
 * */
public class BalanceChangeBuilder {

    private static final int SIGNATURE_FRAGMENTS_LENGTH = Transaction.Field.SIGNATURE_FRAGMENTS.tryteLength;

    public String address;
    public BigInteger value;
    public final StringBuilder signatureOrMessage;
    public final List<TransactionBuilder> buildersFromTailToHead = new LinkedList<>();

    public BalanceChangeBuilder(BalanceChange change) {
        this.address = change.address;
        this.value = change.value;
        this.signatureOrMessage = new StringBuilder(change.signatureOrMessage);
        createTransactionBuildersFromTailToHead();
    }

    public BalanceChangeBuilder(Transaction transaction) {
        this.address = transaction.address;
        this.value = transaction.value;
        this.signatureOrMessage = new StringBuilder(transaction.signatureFragments);
        createTransactionBuildersFromTailToHead();
    }

    public BalanceChangeBuilder(String address, BigInteger value, int amountOfFragments) {
        this.address = address;
        this.value = value;
        this.signatureOrMessage = new StringBuilder(Trytes.fromTrits(new byte[amountOfFragments * SIGNATURE_FRAGMENTS_LENGTH * 3]));
        createTransactionBuildersFromTailToHead();
    }

    public void createTransactionBuildersFromTailToHead() {
        assert buildersFromTailToHead.size() == 0;
        for (int signatureOrMessageOffset = 0; signatureOrMessageOffset < signatureOrMessage.length(); signatureOrMessageOffset += SIGNATURE_FRAGMENTS_LENGTH) {
            boolean isFirstTransaction = signatureOrMessageOffset == 0;
            TransactionBuilder builder = new TransactionBuilder();
            builder.address = address;
            builder.value = isFirstTransaction ? value : BigInteger.ZERO;
            builder.signatureFragments = signatureOrMessage.substring(signatureOrMessageOffset, signatureOrMessageOffset + SIGNATURE_FRAGMENTS_LENGTH);
            buildersFromTailToHead.add(0, builder);
        }
    }

    public void append(Transaction transaction) {
        if (!address.equals(transaction.address))
            throw new IllegalArgumentException("cannot append transaction from different address");
        value = value.add(transaction.value);
        signatureOrMessage.append(transaction.signatureFragments);
    }

    public boolean isInput() {
        return value.compareTo(BigInteger.ZERO) < 0;
    }

    public boolean isOutput() {
        return !isInput();
    }

    public BalanceChange build() {
        return new BalanceChange(address, value, signatureOrMessage.toString());
    }
}
