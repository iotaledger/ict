package org.iota.ict.model;

import com.iota.curl.IotaCurlHash;
import org.iota.ict.utils.Constants;
import org.iota.ict.utils.crypto.SignatureScheme;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * {@link TransferBuilder} is a tool to create a new {@link Transfer} via {@link #build()}.
 */
public final class TransferBuilder {

    private final Set<InputBuilder> inputBuilders;
    private final Set<BalanceChangeBuilder> outputBuilders = new HashSet<>();
    private final int securityLevel;

    public TransferBuilder(Set<InputBuilder> inputBuilders, Set<BalanceChange> outputs, int securityLevel) {
        if(inputBuilders.isEmpty() && outputs.isEmpty())
            throw new IllegalArgumentException("At least one input or output required.");

        this.inputBuilders = inputBuilders;
        for(BalanceChange output : outputs)
            outputBuilders.add(new BalanceChangeBuilder(output.address, output.value, output.signatureOrMessage));

        ensureSumIsZero(inputBuilders, outputBuilders);
        this.securityLevel = securityLevel;
    }

    private static void ensureSumIsZero(Iterable<InputBuilder> inputBuilders, Iterable<BalanceChangeBuilder> outputBuilders) {
        BigInteger sum = BigInteger.ZERO;
        for (InputBuilder signer : inputBuilders) {
            sum = sum.add(signer.getValue());
        }
        for (BalanceChangeBuilder change : outputBuilders) {
            sum = sum.add(change.getValue());
        }
        if (sum.compareTo(BigInteger.ZERO) != 0)
            throw new IllegalArgumentException("Total sum of changes must be 0 but is '" + sum.toString() + "'.");
    }

    public BundleBuilder build() {
        BundleBuilder bundleBuilder = new BundleBuilder();

        List<BalanceChangeBuilderModel> orderedChanges = new LinkedList<>();
        orderedChanges.addAll(inputBuilders);
        orderedChanges.addAll(outputBuilders);
        String determinedBundleHash = determineBundleHash(orderedChanges);

        for (InputBuilder inputBuilder : inputBuilders) {
            inputBuilder.build(determinedBundleHash.substring(0, 27 * securityLevel));
        }
        for (BalanceChangeBuilder outputBuilder : outputBuilders) {
            outputBuilder.build();
        }

        assert orderedChanges.size() > 0;

        for (BalanceChangeBuilderModel changeBuilder : orderedChanges)
            bundleBuilder.append(changeBuilder.getBuildersFromTailToHead());

        return bundleBuilder;
    }

    private static String determineBundleHash(List<BalanceChangeBuilderModel> orderedChanges) {
        StringBuilder concat = new StringBuilder();
        for (BalanceChangeBuilderModel change : orderedChanges)
            for (TransactionBuilder builder : change.getBuildersFromTailToHead()) {
                concat.insert(0, builder.getEssence()).insert(0, change.hasSignature() ? "" : IotaCurlHash.iotaCurlHash(builder.signatureFragments, builder.signatureFragments.length(), Constants.CURL_ROUNDS_BUNDLE_HASH));
            }
        return IotaCurlHash.iotaCurlHash(concat.toString(), concat.length(), Constants.CURL_ROUNDS_BUNDLE_HASH);
    }
}
