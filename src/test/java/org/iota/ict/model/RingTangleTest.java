package org.iota.ict.model;

import org.iota.ict.Ict;
import org.iota.ict.IctTestTemplate;
import org.iota.ict.utils.Properties;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

public class RingTangleTest extends IctTestTemplate {

    @Test
    public void testSameTimestamp() {
        int ringTangleCapacity = 10;

        Properties properties = new Properties();
        properties.tangleCapacity = ringTangleCapacity;
        Ict ict = createIct(properties);

        TransactionBuilder builder = new TransactionBuilder();
        for(int i = 0; i < ringTangleCapacity * 2; i++) {
            ict.submit(builder.build());
            int amountSubmitted = i + 1;
            int expectedTangleSize = Math.min(amountSubmitted+1, ringTangleCapacity); // +1 for NULL transaction
            Assert.assertEquals("Unexpected amount of transactions.", expectedTangleSize, ict.getTangle().size());
        }
    }

    @Test
    public void testMaxNumberOfTransactions() {
        int ringTangleCapacity = 10;
        int offset = ringTangleCapacity / 2;
        int totalTransactions = ringTangleCapacity * 2;

        Properties properties = new Properties();
        properties.tangleCapacity = ringTangleCapacity;
        Ict ict = createIct(properties);

        List<Transaction> transactionsOrderedByTimestamps = generateTransactionsOrderedByTimestamps(totalTransactions);

        Set<Transaction> previousTransactions = new HashSet<>(transactionsOrderedByTimestamps.subList(0, offset + ringTangleCapacity-1));
        Set<Transaction> tangleContentBefore = new HashSet<>(transactionsOrderedByTimestamps.subList(offset, offset + ringTangleCapacity-1));

        Set<Transaction> newTransactions = new HashSet<>(transactionsOrderedByTimestamps.subList(offset + ringTangleCapacity-1, transactionsOrderedByTimestamps.size()));
        Set<Transaction> tangleContentAfter = new HashSet<>(transactionsOrderedByTimestamps.subList(transactionsOrderedByTimestamps.size() - ringTangleCapacity+1, transactionsOrderedByTimestamps.size()));


        submit(ict, previousTransactions);
        assertTangleContainsExactlyPlusNullTx(ict.getTangle(), tangleContentBefore);

        submit(ict, newTransactions);
        assertTangleContainsExactlyPlusNullTx(ict.getTangle(), tangleContentAfter);
    }

    @Test
    public void testInsertIntoSorted() {
        List<Integer> list = new ArrayList<>();
        RingTangle ringTangle = new RingTangle(null, 0);
        Comparator<Integer> integerComparator = new Comparator<Integer>() {
            @Override
            public int compare(Integer integer, Integer t1) {
                return integer.compareTo(t1);
            }
        };

        for(int i = 0; i < 100; i++)
            ringTangle.insertIntoSorted(list, integerComparator, (int)(Math.random() * 1000));

        Integer prevInteger = list.get(0);
        for(Integer i : list) {
            Assert.assertTrue("List not sorted in ascending order: " + prevInteger + " > " + i, prevInteger <= i);
            prevInteger = i;
        }
    }

    private static void submit(Ict ict, Iterable<Transaction> transactions) {
        for(Transaction transaction : transactions)
            ict.submit(transaction);
    }

    private static void assertTangleContainsExactlyPlusNullTx(Tangle tangle, Set<Transaction> transactionsToContain) {
        Assert.assertEquals("Unexpected amount of transactions.", transactionsToContain.size()+1, tangle.size());
        Assert.assertNotNull(tangle.findTransactionLog(Transaction.NULL_TRANSACTION));
        for(Transaction transaction : transactionsToContain)
            Assert.assertNotNull("A transaction is missing.", tangle.findTransactionLog(transaction));
    }

    private static List<Transaction> generateTransactionsOrderedByTimestamps(int amount) {
        List<Transaction> transactions = new LinkedList<>();
        long timestamp = System.currentTimeMillis();
        while (amount > 0) {
            timestamp += Math.random() * 1000 + 1;
            TransactionBuilder builder = new TransactionBuilder();
            builder.issuanceTimestamp = timestamp;
            transactions.add(builder.build());
            amount--;
        }
        return transactions;
    }
}
