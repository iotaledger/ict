package org.iota.ict.model.bundle;

import org.iota.ict.model.transaction.Transaction;
import org.iota.ict.model.transaction.TransactionBuilder;

import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;

/**
 * The {@link BalanceChangeBuilder} makes it possible to accumulate transactions which are part of the same {@link BalanceChange}
 * via {@link #append(Transaction)} or as container of {@link TransactionBuilder} (stored in {@link #buildersFromTailToHead})
 * during the creation of a new {@link Transfer}.
 */
public class BalanceChangeBuilder implements BalanceChangeBuilderModel {

    private static final int SIGNATURE_FRAGMENTS_LENGTH = Transaction.Field.SIGNATURE_FRAGMENTS.tryteLength;

    private final String address;
    private BigInteger value;
    public final StringBuilder signatureOrMessage = new StringBuilder();
    private final List<TransactionBuilder> buildersFromTailToHead;

    public BalanceChangeBuilder(Transaction transaction) {
        this.address = transaction.address();
        this.value = transaction.value;
        signatureOrMessage.append(transaction.signatureFragments());
        this.buildersFromTailToHead = createTransactionBuildersFromTailToHead();
    }

    @Override
    public BigInteger getValue() {
        return value;
    }

    @Override
    public String getAddress() {
        return address;
    }

    @Override
    public List<TransactionBuilder> getBuildersFromTailToHead() {
        return new LinkedList<>(buildersFromTailToHead);
    }

    @Override
    public boolean hasSignature() {
        return false;
    }

    /**
     * Constructor for outputs.
     * */
    public BalanceChangeBuilder(String address, BigInteger value, String message) {
        this.address = address;
        this.value = value;
        this.signatureOrMessage.append(message);
        this.buildersFromTailToHead = createTransactionBuildersFromTailToHead();
    }

    private List<TransactionBuilder> createTransactionBuildersFromTailToHead() {
        List<TransactionBuilder> buildersFromTailToHead = new LinkedList<>();
        for (int signatureOrMessageOffset = 0; signatureOrMessageOffset < signatureOrMessage.length(); signatureOrMessageOffset += SIGNATURE_FRAGMENTS_LENGTH) {
            boolean isFirstTransaction = signatureOrMessageOffset == 0;
            TransactionBuilder builder = new TransactionBuilder();
            builder.address = address;
            builder.value = isFirstTransaction ? value : BigInteger.ZERO;
            builder.signatureFragments = signatureOrMessage.substring(signatureOrMessageOffset, signatureOrMessageOffset + SIGNATURE_FRAGMENTS_LENGTH);
            buildersFromTailToHead.add(0, builder);
        }
        assert buildersFromTailToHead.size() > 0;
        return buildersFromTailToHead;
    }

    public void append(Transaction transaction) {
        if (!address.equals(transaction.address()))
            throw new IllegalArgumentException("cannot append transaction from different address");
        value = value.add(transaction.value);
        signatureOrMessage.append(transaction.signatureFragments());
    }

    public boolean isInput() {
        return value.compareTo(BigInteger.ZERO) < 0;
    }

    public boolean isOutput() {
        return !isInput();
    }

    public BalanceChange build() {
        if(buildersFromTailToHead.size() == 0)
            throw new IllegalStateException("Empty: no transactions.");
        assert signatureOrMessage.length()%Transaction.Field.SIGNATURE_FRAGMENTS.tryteLength == 0;
        return new BalanceChange(address, value, signatureOrMessage.toString());
    }
}
