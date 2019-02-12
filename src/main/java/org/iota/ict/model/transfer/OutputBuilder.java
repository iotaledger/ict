package org.iota.ict.model.transfer;

import com.iota.curl.IotaCurlHash;
import org.iota.ict.model.bc.BalanceChangeBuilder;
import org.iota.ict.model.transaction.Transaction;
import org.iota.ict.model.transaction.TransactionBuilder;
import org.iota.ict.utils.Constants;

import java.math.BigInteger;

public class OutputBuilder extends BalanceChangeBuilder {

    public OutputBuilder(String address, BigInteger value, String message) {
        super(address, value, (int)Math.ceil(message.length() * 1.0 / Transaction.Field.SIGNATURE_FRAGMENTS.tryteLength));
        if(value.compareTo(BigInteger.ZERO) < 0)
            throw new IllegalArgumentException("Value must be positive or zero in output.");
        for(int fragmentIndex = 0; (fragmentIndex+1) * Transaction.Field.SIGNATURE_FRAGMENTS.tryteLength < message.length(); fragmentIndex++) {
            String fragment = message.substring(fragmentIndex * Transaction.Field.SIGNATURE_FRAGMENTS.tryteLength, (fragmentIndex+1)*Transaction.Field.SIGNATURE_FRAGMENTS.tryteLength);
            buildersFromTailToHead[fragmentIndex].signatureFragments = fragment;
        }
    }

    @Override
    public String getEssence() {
        StringBuilder essence = new StringBuilder();
        for (TransactionBuilder builder : buildersFromTailToHead) {
            essence.insert(0, builder.getEssence()).insert(0, IotaCurlHash.iotaCurlHash(builder.signatureFragments, builder.signatureFragments.length(), Constants.CURL_ROUNDS_BUNDLE_HASH));
        }
        return essence.toString();
    }
}
