package org.iota.ict.model;

import org.iota.ict.Ict;
import org.iota.ict.utils.Properties;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

public class BundleTest {

    private Ict ict;

    @Test
    public void testBundleWithValidStructure() {
        ict = new Ict(new Properties());
        testBundleWithValidStructure(1);
        testBundleWithValidStructure(6);
    }

    private void testBundleWithValidStructure(int size) {

        int bundleSize = 6;
        LinkedList<Transaction> referenceFromTailToHead = buildBundle(bundleSize);
        submitBundle(ict, referenceFromTailToHead);

        String headHash = referenceFromTailToHead.getLast().hash;
        Transaction head = ict.getTangle().findTransactionByHash(headHash);
        Bundle bundle = new Bundle(head);

        LinkedList<Transaction> referenceFromHeadToTail = new LinkedList<>(referenceFromTailToHead);
        Collections.reverse(referenceFromHeadToTail);
        validateBundle(bundle, referenceFromHeadToTail);
    }

    public void validateBundle(Bundle bundle, List<Transaction> reference) {
        Assert.assertTrue("bundle is not complete", bundle.isComplete());
        Assert.assertTrue("bundle structure was recognized as invalid but is valid", bundle.isStructureValid());

        List<Transaction> resulFromHeadToTail = bundle.getTransactions();
        Assert.assertEquals("bundle size is incorrect", reference.size(), resulFromHeadToTail.size());
        for(int i = 0; i < reference.size(); i++) {
            Assert.assertEquals("bundle was linked together incorrectly", reference.get(i).hash, resulFromHeadToTail.get(i).hash);
        }
    }

    @Test
    public void testBundleWithInvalidStructure() {

        ict = new Ict(new Properties());

        int bundleSize = 6;
        LinkedList<Transaction> referenceFromTailToHead = buildInvalidBundle(bundleSize);
        submitBundle(ict, referenceFromTailToHead);

        String headHash = referenceFromTailToHead.getLast().hash;
        Transaction head = ict.getTangle().findTransactionByHash(headHash);
        Bundle bundle = new Bundle(head);

        Assert.assertTrue("bundle is not complete", bundle.isComplete());
        Assert.assertTrue("bundle structure was recognized as valid but is invalid", !bundle.isStructureValid());
    }

    @After
    public void tearDown() {
        if(ict != null)
            ict.terminate();
    }

    private static void submitBundle(Ict ict, Collection<Transaction> bundle) {
        Set<Transaction> bundleRandomOrder = new HashSet<>(bundle);
        for(Transaction transaction : bundleRandomOrder)
            ict.submit(transaction);
    }

    private LinkedList<Transaction> buildInvalidBundle(int bundleSize) {
        LinkedList<Transaction> bundleList = new LinkedList<>();
        bundleList.add(buildHead(Transaction.NULL_TRANSACTION.hash)); // invalid because head instead of tail
        for(int i = 0; i < bundleSize-2; i++)
            bundleList.add(buildInner(bundleList.getLast().hash));
        bundleList.add(buildHead(bundleList.getLast().hash));
        assert bundleList.size() == bundleSize;
        return bundleList;
    }

    /**
     * @param bundleSize Size of the bundle. Must be positive.
     * @throws IllegalArgumentException if bundleSize is not positive.
     * */
    private LinkedList<Transaction> buildBundle(int bundleSize) {
        if(bundleSize == 1)
            return buildSingleTransactionBundle();
        if(bundleSize < 1)
            throw new IllegalArgumentException("bundle size must be positive.");
        LinkedList<Transaction> bundleList = new LinkedList<>();
        bundleList.add(buildTail());
        for(int i = 0; i < bundleSize-2; i++)
            bundleList.add(buildInner(bundleList.getLast().hash));
        bundleList.add(buildHead(bundleList.getLast().hash));
        assert bundleList.size() == bundleSize;
        return bundleList;
    }

    /**
     * Builds a bundle consisting of exactly one transaction. Bundle head is bundle tail.
     * */
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