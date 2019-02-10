package org.iota.ict.model;

import org.iota.ict.utils.crypto.SignatureSchemeImplementation;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

public class InputBuilder implements  BalanceChangeBuilderModel {

    private final BigInteger value;
    private final SignatureSchemeImplementation.PrivateKey privateKey;
    private final String address;
    private final TransactionBuilder[] buildersFromTailToHead;

    public InputBuilder(SignatureSchemeImplementation.PrivateKey privateKey, BigInteger value, int fragments) {
        if(privateKey == null)
            throw new NullPointerException("private key");
        if(value == null)
            throw new NullPointerException("value");
        if(fragments <= 0)
            throw new IllegalArgumentException("fragments must be positive");
        this.privateKey = privateKey;
        this.value = value;
        this.address = privateKey.deriveAddress();
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

    @Override
    public boolean hasSignature() {
        return true;
    }

    public BalanceChange build(String bundleHash) {
        SignatureSchemeImplementation.Signature signature = privateKey.sign(bundleHash);
        if(signature.fragments() != buildersFromTailToHead.length)
            throw new IllegalStateException("BalanceChange has reserved " + buildersFromTailToHead.length + " transactions but signature length is " + signature.length() + " trytes.");
        for(int i = 0; i < buildersFromTailToHead.length; i++) {
            SignatureSchemeImplementation.Signature signatureFragment = signature.getFragment(i);
            buildersFromTailToHead[i].signatureFragments = signatureFragment.toString();
        }
        return new BalanceChange(address, value, signature.toString());
    }
}
