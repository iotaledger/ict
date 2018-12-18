package org.iota.ict.model;

import org.iota.ict.Ict;
import org.iota.ict.IctTestTemplate;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

public class BundleTest extends IctTestTemplate {

    @Test
    public void testBundleWithValidStructure() {
        Ict ict = createIct();
        testBundleWithValidStructure(ict, 1);
        testBundleWithValidStructure(ict, (int) (Math.random() * 6) + 2);
    }

    private void testBundleWithValidStructure(Ict ict, int bundleSize) {

        LinkedList<Transaction> referenceFromTailToHead = buildBundle(bundleSize);
        submitTransactions(ict, referenceFromTailToHead);

        String headHash = referenceFromTailToHead.getLast().hash;
        Transaction head = ict.getTangle().findTransactionByHash(headHash);
        Bundle bundle = new Bundle(head);

        LinkedList<Transaction> referenceFromHeadToTail = new LinkedList<>(referenceFromTailToHead);
        Collections.reverse(referenceFromHeadToTail);
        validateBundle(bundle, referenceFromHeadToTail);
    }

    @Test
    public void testBundleBuilding() {
        Ict a = createIct();
        Ict b = createIct();
        connect(a, b);

        int postCutPoisition = 2;
        int bundleSize = 6;

        LinkedList<Transaction> referenceFromTailToHead = buildBundle(bundleSize);
        List<Transaction> bundleTailIncludingSubPart = referenceFromTailToHead.subList(0, postCutPoisition);
        List<Transaction> bundleHeadIncludingSubPart = referenceFromTailToHead.subList(postCutPoisition, bundleSize);

        submitTransactions(a, bundleHeadIncludingSubPart);
        waitUntilCommunicationEnds(100);

        String headHash = referenceFromTailToHead.getLast().hash;
        Transaction head = b.getTangle().findTransactionByHash(headHash);
        Assert.assertNotNull("did not receive head", head);

        Bundle bundle = new Bundle(head);
        Assert.assertFalse("bundle recognized as complete before all transactions were received", bundle.isComplete());

        submitTransactions(a, bundleTailIncludingSubPart);
        waitUntilCommunicationEnds(100);
        Assert.assertEquals("did not receive all bundle transactions", a.getTangle().size(), b.getTangle().size());

        bundle.build();
        LinkedList<Transaction> referenceFromHeadToTail = new LinkedList<>(referenceFromTailToHead);
        Collections.reverse(referenceFromHeadToTail);
        validateBundle(bundle, referenceFromHeadToTail);
    }

    public void validateBundle(Bundle bundle, List<Transaction> reference) {
        Assert.assertTrue("bundle is not complete", bundle.isComplete());
        Assert.assertTrue("bundle structure was recognized as invalid but is valid", bundle.isStructureValid());

        List<Transaction> resulFromHeadToTail = bundle.getTransactions();
        Assert.assertEquals("bundle size is incorrect", reference.size(), resulFromHeadToTail.size());
        for (int i = 0; i < reference.size(); i++) {
            Assert.assertEquals("bundle was linked together incorrectly", reference.get(i).hash, resulFromHeadToTail.get(i).hash);
        }
    }

    @Test
    public void testBundleWithInvalidStructure() {

        Ict ict = createIct();

        int bundleSize = 6;
        LinkedList<Transaction> referenceFromTailToHead = buildInvalidBundle(bundleSize);
        submitTransactions(ict, referenceFromTailToHead);

        String headHash = referenceFromTailToHead.getLast().hash;
        Transaction head = ict.getTangle().findTransactionByHash(headHash);
        Bundle bundle = new Bundle(head);

        Assert.assertTrue("bundle is not complete", bundle.isComplete());
        Assert.assertTrue("bundle structure was recognized as valid but is invalid", !bundle.isStructureValid());
    }

    private static void submitTransactions(Ict ict, Collection<Transaction> transactions) {
        Set<Transaction> bundleRandomOrder = new HashSet<>(transactions);
        for (Transaction transaction : bundleRandomOrder)
            ict.submit(transaction);
    }

    private LinkedList<Transaction> buildInvalidBundle(int bundleSize) {
        LinkedList<Transaction> bundleList = new LinkedList<>();
        bundleList.add(buildHead(Transaction.NULL_TRANSACTION.hash)); // invalid because head instead of tail
        for (int i = 0; i < bundleSize - 2; i++)
            bundleList.add(buildInner(bundleList.getLast().hash));
        bundleList.add(buildHead(bundleList.getLast().hash));
        assert bundleList.size() == bundleSize;
        return bundleList;
    }

    /**
     * @param bundleSize Size of the bundle. Must be positive.
     * @throws IllegalArgumentException if bundleSize is not positive.
     */
    private LinkedList<Transaction> buildBundle(int bundleSize) {
        if (bundleSize == 1)
            return buildSingleTransactionBundle();
        if (bundleSize < 1)
            throw new IllegalArgumentException("bundle size must be positive.");
        LinkedList<Transaction> bundleList = new LinkedList<>();
        bundleList.add(buildTail());
        for (int i = 0; i < bundleSize - 2; i++)
            bundleList.add(buildInner(bundleList.getLast().hash));
        bundleList.add(buildHead(bundleList.getLast().hash));
        assert bundleList.size() == bundleSize;
        return bundleList;
    }

    /**
     * Builds a bundle consisting of exactly one transaction. Bundle head is bundle tail.
     */
    private static LinkedList<Transaction> buildSingleTransactionBundle() {
        LinkedList<Transaction> bundleList = new LinkedList<>();
        TransactionBuilder builder = new TransactionBuilder();
        builder.isBundleHead = true;
        builder.isBundleTail = true;
        bundleList.add(builder.build());
        return bundleList;
    }

    private static Transaction buildHead(String last) {
        TransactionBuilder builder = new TransactionBuilder();
        builder.trunkHash = last;
        builder.isBundleHead = true;
        builder.isBundleTail = false;
        return builder.build();
    }

    private static Transaction buildInner(String last) {
        TransactionBuilder builder = new TransactionBuilder();
        builder.trunkHash = last;
        builder.isBundleHead = false;
        builder.isBundleTail = false;
        return builder.build();
    }

    private static Transaction buildTail() {
        TransactionBuilder builder = new TransactionBuilder();
        builder.isBundleHead = false;
        builder.isBundleTail = true;
        return builder.build();
    }
}