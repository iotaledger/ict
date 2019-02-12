package org.iota.ict.model.transfer;

import org.iota.ict.model.bc.BalanceChange;
import org.iota.ict.model.bc.BalanceChangeBuilder;
import org.iota.ict.model.transaction.Transaction;
import org.iota.ict.model.transaction.TransactionBuilder;
import org.iota.ict.utils.crypto.SignatureSchemeImplementation;

import java.math.BigInteger;

public class InputBuilder extends BalanceChangeBuilder {

    private final SignatureSchemeImplementation.PrivateKey privateKey;

    public InputBuilder(SignatureSchemeImplementation.PrivateKey privateKey, BigInteger value) {
        super(privateKey.deriveAddress(), value, privateKey.length() / Transaction.Field.SIGNATURE_FRAGMENTS.tryteLength);
        this.privateKey = privateKey;
        if(value.compareTo(BigInteger.ZERO) >= 0)
            throw new IllegalArgumentException("Value must be negative in input.");
    }

    public BalanceChange build(String bundleHash) {
        SignatureSchemeImplementation.Signature signature = privateKey.sign(bundleHash);
        if(signature.fragments() != buildersFromTailToHead.length)
            throw new IllegalStateException("BalanceChange has reserved " + buildersFromTailToHead.length + " transactions but signature length is " + signature.length() + " trytes.");
        for(int i = 0; i < buildersFromTailToHead.length; i++) {
            SignatureSchemeImplementation.Signature signatureFragment = signature.getFragment(i);
            buildersFromTailToHead[i].signatureFragments = signatureFragment.toString();
        }
        return new BalanceChange(getAddress(), getValue(), signature.toString());
    }

    @Override
    public String getEssence() {
        StringBuilder essence = new StringBuilder();
        for (TransactionBuilder builder : buildersFromTailToHead) {
            essence.insert(0, builder.getEssence());
        }
        return essence.toString();
    }
}
