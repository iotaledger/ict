package org.iota.ict.model;

import java.math.BigInteger;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class BalanceChange {

    private static final int SIGNATURE_FRAGMENTS_LENGTH = Transaction.Field.SIGNATURE_FRAGMENTS.tryteLength;

    public final String address;
    public final BigInteger value;
    public final String signatureOrMessage;

    public BalanceChange(String address, BigInteger value, String signatureOrMessage) {
        if(signatureOrMessage.length()%SIGNATURE_FRAGMENTS_LENGTH != 0)
            throw new IllegalArgumentException("length of signatureOrMessage must be multiple of "+SIGNATURE_FRAGMENTS_LENGTH);
        this.address = address;
        this.value = value;
        this.signatureOrMessage = signatureOrMessage;
    }

    public void appendToBundleBuilder(BundleBuilder bundleBuilder) {
        List<TransactionBuilder> buildersFromHeadToTail = new LinkedList<>();
        for(int signatureOrMessageOffset = 0; signatureOrMessageOffset < signatureOrMessage.length(); signatureOrMessageOffset += SIGNATURE_FRAGMENTS_LENGTH) {
            boolean isFirstTransaction = signatureOrMessageOffset == 0;
            TransactionBuilder builder = new TransactionBuilder();
            builder.address = address;
            builder.value = isFirstTransaction ? value : BigInteger.ZERO;
            builder.signatureFragments = signatureOrMessage.substring(signatureOrMessageOffset, signatureOrMessageOffset + SIGNATURE_FRAGMENTS_LENGTH);
            buildersFromHeadToTail.add(builder);
        }
        List<TransactionBuilder> buildersFromTailToHead = new LinkedList<>(buildersFromHeadToTail);
        Collections.reverse(buildersFromTailToHead);
        bundleBuilder.append(buildersFromTailToHead);
    }


    @Override
    public boolean equals(Object o) {
        if(o instanceof BalanceChange) {
            BalanceChange bc = (BalanceChange) o;
            return address.equals(bc.address) && value.equals(bc.value) && signatureOrMessage.equals(bc.signatureOrMessage);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (address+signatureOrMessage+value).hashCode();
    }
}
