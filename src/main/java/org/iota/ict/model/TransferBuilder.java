package org.iota.ict.model;

import com.iota.curl.IotaCurlHash;
import org.iota.ict.utils.Constants;
import org.iota.ict.utils.Trytes;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public final class TransferBuilder {

    private final Set<BalanceChangeBuilder> inputBuilders;
    private final Set<BalanceChangeBuilder> outputBuilders;
    private final int securityLevel;

    public static Bundle buildBundle(Set<BalanceChange> changes, int securityLevel) {
        Set<BalanceChangeBuilder> changeBuilders = new HashSet<>();
        for (BalanceChange change : changes)
            changeBuilders.add(change.isInput() ? new BalanceChangeBuilder(change.address, change.value, securityLevel) : new BalanceChangeBuilder(change)); // TODO multisig requires more fragments
        return new TransferBuilder(changeBuilders, securityLevel).build();
    }

    private TransferBuilder(Set<BalanceChangeBuilder> changeBuilders, int securityLevel) {
        ensureSumIsZero(changeBuilders);
        inputBuilders = filterOutChangeBuildersWithNegativeValue(changeBuilders);
        // outputBuilders are all changeBuilders which are not inputBuilders
        outputBuilders = new HashSet<>(changeBuilders);
        outputBuilders.removeAll(inputBuilders);
        this.securityLevel = securityLevel;
    }

    private static void ensureSumIsZero(Iterable<BalanceChangeBuilder> changeBuilders) {
        BigInteger sum = BigInteger.ZERO;
        for (BalanceChangeBuilder change : changeBuilders) {
            sum = sum.add(change.value);
        }
        if (sum.compareTo(BigInteger.ZERO) != 0)
            throw new IllegalArgumentException("Total sum of changes must be 0 but is '" + sum.toString() + "'.");
    }

    private static Set<BalanceChangeBuilder> filterOutChangeBuildersWithNegativeValue(Iterable<BalanceChangeBuilder> changeBuilders) {
        Set<BalanceChangeBuilder> inputs = new HashSet<>();
        for (BalanceChangeBuilder change : changeBuilders)
            if (change.value.compareTo(BigInteger.ZERO) < 0)
                inputs.add(change);
        return inputs;
    }

    private Bundle build() {
        BundleBuilder bundleBuilder = new BundleBuilder();

        List<BalanceChangeBuilder> orderedChanges = new LinkedList<>(inputBuilders);
        orderedChanges.addAll(outputBuilders);

        String determinedBundleHash = determineBundleHash(orderedChanges);;
        createAllSignatures(determinedBundleHash, orderedChanges);

        for (BalanceChangeBuilder changeBuilder : orderedChanges)
            bundleBuilder.append(changeBuilder.buildersFromTailToHead);

        Bundle bundle = bundleBuilder.build();
        assert determinedBundleHash.equals(bundle.getHash());
        return bundleBuilder.build();
    }

    private void createAllSignatures(String bundleHash, List<BalanceChangeBuilder> orderedChanges) {
        for (BalanceChangeBuilder inputBuilder : inputBuilders) {
            inputBuilder.signatureOrMessage.setLength(0);
            inputBuilder.signatureOrMessage.append(createSignature(bundleHash));
        }
    }

    private String createSignature(String bundleHash) {
        StringBuilder signature = new StringBuilder();
        for (int i = 0; i < securityLevel; i++)
            signature.append(signTrytes(bundleHash.substring(27 * i, 27 * i + 27)));
        return signature.toString();
    }

    private String signTrytes(String bundleHash) {
        // TODO implement
        return Trytes.randomSequenceOfLength(Transaction.Field.SIGNATURE_FRAGMENTS.tryteLength);
    }

    static String determineBundleHash(List<BalanceChangeBuilder> orderedChanges) {
        StringBuilder concat = new StringBuilder();
        for (BalanceChangeBuilder change : orderedChanges)
            for (TransactionBuilder builder : change.buildersFromTailToHead) {
                concat.insert(0, builder.getEssence()).insert(0, change.isOutput() ? IotaCurlHash.iotaCurlHash(builder.signatureFragments, builder.signatureFragments.length(), Constants.CURL_ROUNDS_BUNDLE_HASH) : "");
            }
        return IotaCurlHash.iotaCurlHash(concat.toString(), concat.length(), Constants.CURL_ROUNDS_BUNDLE_HASH);
    }
}
