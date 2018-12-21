package org.iota.ict.model;

import java.math.BigInteger;

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
        return signatureOrMessage.substring(index * SIGNATURE_FRAGMENTS_LENGTH + (index + 1) * SIGNATURE_FRAGMENTS_LENGTH);
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
