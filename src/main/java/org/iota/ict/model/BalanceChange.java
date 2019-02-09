package org.iota.ict.model;

import java.math.BigInteger;

/**
 * A {@link BalanceChange} models a proposed change for the IOTA token balance of an IOTA address. Since tokens can neither be
 * burned nor created, no positive or negative {@link BalanceChange} cannot exist on its own but requires other balance
 * changes so the sum of their proposed changes is zero. They are grouped together in a {@link Transfer}.
 * <p>
 * Depending on its proposed change, each instance is either an input or an output (see {@link #isInput()} and {@link #isOutput()}).
 * Inputs have a negative {@link #value} (they remove funds from an address so that another address can receive them). Outputs have a positive or a zero {@link #value}.
 * In inputs, the {@link #signatureOrMessage} is used as signature signing the proposed change with the addresses private key,
 * in outputs as optional message.
 * <p>
 * Each {@link BalanceChange} is realized through at least one transactions. The required amount depends on the length
 * of {@link #signatureOrMessage}.
 *
 * @see Transfer as structure consisting of multiple {@link BalanceChange} instances.
 * @see BalanceChangeBuilder as builder for this class.
 */
public class BalanceChange {

    private static final int SIGNATURE_FRAGMENTS_LENGTH = Transaction.Field.SIGNATURE_FRAGMENTS.tryteLength;

    public final String address;
    public final BigInteger value;
    public final String signatureOrMessage;

    public BalanceChange(String address, BigInteger value, String signatureOrMessage) {
        if (signatureOrMessage.length() % SIGNATURE_FRAGMENTS_LENGTH != 0)
            throw new IllegalArgumentException("length of signatureOrMessage must be multiple of " + SIGNATURE_FRAGMENTS_LENGTH);
        this.address = address;
        this.value = value;
        this.signatureOrMessage = signatureOrMessage;
    }

    public int getAmountOfSignatureOrMessageFragments() {
        return signatureOrMessage.length() / SIGNATURE_FRAGMENTS_LENGTH;
    }

    public String getSignatureOrMessageFragment(int index) {
        assert index >= 0 && (index + 1) * SIGNATURE_FRAGMENTS_LENGTH <= signatureOrMessage.length();
        return signatureOrMessage.substring(index * SIGNATURE_FRAGMENTS_LENGTH, (index + 1) * SIGNATURE_FRAGMENTS_LENGTH);
    }

    public String getSignatureOrMessage() {
        return signatureOrMessage;
    }

    public boolean isInput() {
        return value.compareTo(BigInteger.ZERO) < 0;
    }

    public boolean isOutput() {
        return !isInput();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof BalanceChange) {
            BalanceChange bc = (BalanceChange) o;
            return address.equals(bc.address) && value.equals(bc.value) && signatureOrMessage.equals(bc.signatureOrMessage);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (address + signatureOrMessage + value).hashCode();
    }
}
