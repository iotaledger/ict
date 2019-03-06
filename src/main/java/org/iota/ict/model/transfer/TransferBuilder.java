package org.iota.ict.model.transfer;

import com.iota.curl.IotaCurlHash;
import org.iota.ict.model.bundle.BundleBuilder;
import org.iota.ict.model.bc.BalanceChangeBuilderInterface;
import org.iota.ict.utils.Constants;

import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * {@link TransferBuilder} is a tool to create a new {@link Transfer} via {@link #build()}.
 */
public final class TransferBuilder {

    private final Set<InputBuilder> inputBuilders;
    private final Set<OutputBuilder> outputs;
    private final int securityLevel;

    public TransferBuilder(Set<InputBuilder> inputBuilders, Set<OutputBuilder> outputs, int securityLevel) {
        if(inputBuilders.isEmpty() && outputs.isEmpty())
            throw new IllegalArgumentException("At least one input or output required.");

        this.inputBuilders = inputBuilders;
        this.outputs = outputs;

        ensureSumIsZero(inputBuilders, outputs);
        this.securityLevel = securityLevel;
    }

    private static void ensureSumIsZero(Iterable<InputBuilder> inputBuilders, Iterable<OutputBuilder> outputs) {
        BigInteger sum = BigInteger.ZERO;
        for (InputBuilder signer : inputBuilders) {
            sum = sum.add(signer.getValue());
        }
        for (OutputBuilder output : outputs) {
            sum = sum.add(output.getValue());
        }
        if (sum.compareTo(BigInteger.ZERO) != 0)
            throw new IllegalArgumentException("Total sum of changes must be 0 but is '" + sum.toString() + "'.");
    }

    public BundleBuilder build() {
        BundleBuilder bundleBuilder = new BundleBuilder();

        List<BalanceChangeBuilderInterface> orderedChanges = new LinkedList<>();
        orderedChanges.addAll(inputBuilders);
        orderedChanges.addAll(outputs);
        String determinedBundleHash = determineBundleHash(orderedChanges);

        for (InputBuilder inputBuilder : inputBuilders) {
            inputBuilder.build(determinedBundleHash.substring(0, 27 * securityLevel));
        }

        assert orderedChanges.size() > 0;

        for (BalanceChangeBuilderInterface changeBuilder : orderedChanges)
            bundleBuilder.append(changeBuilder.getBuildersFromTailToHead());

        return bundleBuilder;
    }

    private static String determineBundleHash(List<BalanceChangeBuilderInterface> orderedChanges) {
        StringBuilder concat = new StringBuilder();
        for (BalanceChangeBuilderInterface change : orderedChanges)
            concat.insert(0, change.getEssence());
        return IotaCurlHash.iotaCurlHash(concat.toString(), concat.length(), Constants.CURL_ROUNDS_BUNDLE_HASH);
    }
}
