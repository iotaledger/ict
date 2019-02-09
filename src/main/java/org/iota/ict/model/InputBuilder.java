package org.iota.ict.model;

import org.iota.ict.utils.crypto.SignatureScheme;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

public class InputBuilder implements  BalanceChangeBuilderModel {

    private final BigInteger value;
    private final String privateKey;
    private final String address;
    private final TransactionBuilder[] buildersFromTailToHead;

    public InputBuilder(String privateKey, BigInteger value, int fragments) {
        if(privateKey == null)
            throw new NullPointerException("private key");
        if(value == null)
            throw new NullPointerException("value");
        if(fragments <= 0)
            throw new IllegalArgumentException("fragments must be positive");
        this.privateKey = privateKey;
        this.value = value;
        this.address = SignatureScheme.deriveAddressFromPrivateKey(privateKey);
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

    public BalanceChange build(String bundleHash, int securityLevel) {
        String signature = SignatureScheme.sign(privateKey, bundleHash, securityLevel);
        if(signature.length() != buildersFromTailToHead.length * Transaction.Field.SIGNATURE_FRAGMENTS.tryteLength)
            throw new IllegalStateException("BalanceChange has reserved " + buildersFromTailToHead.length + " transactions but signature length is " + signature.length() + " trytes.");
        for(int i = 0; i < buildersFromTailToHead.length; i++) {
            String signatureFragment = signature.substring(i * Transaction.Field.SIGNATURE_FRAGMENTS.tryteLength, (i+1)*Transaction.Field.SIGNATURE_FRAGMENTS.tryteLength);
            buildersFromTailToHead[i].signatureFragments = signatureFragment;
        }
        return new BalanceChange(address, value, signature);
    }
}
