package org.iota.ict.model.tangle;

import org.iota.ict.Ict;
import org.iota.ict.model.transaction.Transaction;
import org.iota.ict.model.transaction.TransactionBuilder;
import org.iota.ict.utils.Trytes;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ConcurrentModificationException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.fail;

/**
 * Unit test for {@link Tangle}
 */
@Ignore // Test occasionally. Takes too long.
public class TangleTest {
    private final Ict ictMock = Mockito.mock(Ict.class);

    private final Tangle underTest = new Tangle(ictMock);

    @Test
    public void when_construct_with_nullIct_then_throw_NPE() {
        try {
            new Tangle(null);
            fail("NPE (\"'ict' must not null\") expected.");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof NullPointerException);
            Assert.assertEquals("'ict' must not null", e.getMessage());
        }
    }

    @Test
    public void when_findTransactionByAddress_then_sameCreatedTransaction_expected() {
        // given
        Transaction expected = randomTransaction();

        // when
        underTest.createTransactionLogIfAbsent(expected);

        //then
        Set<Transaction> actual = underTest.findTransactionsByAddress(expected.address());
        assertThatTransactionContainedInSet(expected, actual);
    }

    @Test
    public void when_remove_transaction_concurrently_no_error_occur() throws InterruptedException {
        // given
        final int transactionCountWhereConcurrentAccessExpected = 200;
        final String transactionsForAddress = Trytes.randomSequenceOfLength(81);
        final Set<Transaction> transactionSet = createRandomTransactionsForOneAddress(transactionCountWhereConcurrentAccessExpected, transactionsForAddress);
        final AtomicBoolean concurrentModifcationObserver = new AtomicBoolean(false);

        // when
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Set<Transaction> currentSet = underTest.findTransactionsByAddress(transactionsForAddress);
                        safeSleep(13);
                    } catch (ConcurrentModificationException e) {
                        System.err.println("\n>>>> D E T E C T - " + e.getClass() + " cause " + e.getMessage() + "\n");
                        concurrentModifcationObserver.set(true);
                        e.printStackTrace();
                        break;
                    }
                }
            }
        }, "Break-on-first-conccurent-exception-thread").start();

        final CountDownLatch blocksUntilAllTransactionsRemoved = new CountDownLatch(1);
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (Transaction aTransactionSet : transactionSet) {
                    try {
                        underTest.deleteTransaction(aTransactionSet);

                        safeSleep(7);
                    } catch (Exception e) {
                        // Should never occur
                        e.printStackTrace();
                    }
                }
                blocksUntilAllTransactionsRemoved.countDown();
            }
        },"Delete-all-transactions-thread").start();

        blocksUntilAllTransactionsRemoved.await();
        // then check concurrent access
        // - its no 100 % test case because the test can also success in error case.
        // - it depends if the test threads really access the transaction-list at the same time
        Assert.assertFalse("Expect no concurrent modification exception.", concurrentModifcationObserver.get());
    }


    @Test
    public void findTransactionsByAddressThreadSafety() throws InterruptedException {
        // first thread : call findTransactionByAddress for a given address in a loop
        // second thread : create a transaction to that address, then delete it

        // given
        final Transaction aTransaction = randomTransaction();
        final String transactionAddress = aTransaction.address();
        underTest.createTransactionLogIfAbsent(aTransaction);

        final AtomicBoolean exceptionOccurred = new AtomicBoolean(false);
        // when
        new Thread(new Runnable() {
            @Override
            public void run() {

                while (true) {
                    try {
                        Set<Transaction> currentSet = underTest.findTransactionsByAddress(transactionAddress);
                        //safeSleep(1);
                    } catch (Exception e) {
                        e.printStackTrace();
                        exceptionOccurred.set(true);
                        fail("findTransactionsByAddress should never throw an exception");
                        break;
                    }
                }
            }
        }, "Break-on-first-conccurent-exception-thread").start();

        final CountDownLatch blocksUntilAllTransactionsRemoved = new CountDownLatch(1);
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i=0;i<50;i++) {
                    try {
                        underTest.deleteTransaction(aTransaction);
                        safeSleep(1);
                        underTest.createTransactionLogIfAbsent(aTransaction);
                        safeSleep(1);
                    } catch (Exception e) {
                        // Should never occur
                        e.printStackTrace();
                    }
                    if(exceptionOccurred.get()){
                        System.err.println("Saw exception after "+i+" loops");
                        break;
                    }
                }
                blocksUntilAllTransactionsRemoved.countDown();
            }
        },"Delete-all-transactions-thread").start();

        blocksUntilAllTransactionsRemoved.await();
        if(exceptionOccurred.get()){
            fail("findTransactionsByAddress should not fail");
        }
    }

    /*
     *************************************
     * Private helper
     *************************************
     */

    private Set<Transaction> createRandomTransactionsForOneAddress(int transactionCountWhereCunncurrentExceptionAlwaysOccure, String address) {
        final Set<Transaction> transactions = randomSetOfTransactionsForTheSameAddress(transactionCountWhereCunncurrentExceptionAlwaysOccure, address);
        for (Transaction transaction : transactions) {
            underTest.createTransactionLogIfAbsent(transaction);
        }
        final Set<Transaction> actualSet = underTest.findTransactionsByAddress(address);
        Assert.assertEquals(transactions.size(), actualSet.size());
        return actualSet;
    }

    private static void safeSleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static Transaction randomTransaction() {
        TransactionBuilder builder = new TransactionBuilder();
        builder.address = Trytes.randomSequenceOfLength(81);
        Transaction transaction = builder.build();
        return transaction;
    }

    private static Set<Transaction> randomSetOfTransactionsForTheSameAddress(int size, String address) {
        final TransactionBuilder builder = new TransactionBuilder();
        builder.address = address;

        LinkedHashSet<Transaction> randomSet = new LinkedHashSet<>();
        for (int i = 0; i < size; i++) {
            builder.asciiMessage("Ascii message " + i);
            randomSet.add(builder.build());
        }
        return randomSet;
    }

    private static Set<Transaction> assertThatTransactionContainedInSet(Transaction expected, Set<Transaction> actual) {
        Assert.assertNotNull("Expect not null transaction result set.", actual);
        Assert.assertTrue("Expect same created transaction instance", actual.contains(expected));
        return actual;
    }
}
