package org.iota.ict.ec;

import org.iota.ict.Ict;
import org.iota.ict.IctTestTemplate;
import org.iota.ict.model.bundle.Bundle;
import org.iota.ict.model.bundle.BundleBuilder;
import org.iota.ict.model.transaction.Transaction;
import org.iota.ict.model.transaction.TransactionBuilder;
import org.iota.ict.model.transfer.InputBuilder;
import org.iota.ict.model.transfer.OutputBuilder;
import org.iota.ict.model.transfer.TransferBuilder;
import org.iota.ict.utils.Trytes;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LedgerValidatorTest extends IctTestTemplate {

    @Test
    public void testIncompleteTangle() {
        Ict ict = createIct();
        LedgerValidator validator = new LedgerValidator(ict);

        TransactionBuilder builder = new TransactionBuilder();
        Transaction transaction1 = builder.build();
        builder.trunkHash = transaction1.branchHash();
        Transaction transaction2 = builder.build();
        ict.submit(transaction2);
        saveSleep(50);

        try {
            validator.isTangleValid(transaction1.hash);
            Assert.fail("No exception thrown despite incomplete Tangle.");
        } catch (LedgerValidator.IncompleteTangleException incompleteTangleException) {
            Assert.assertEquals("Wrong transaction reported as missing.", transaction1.hash, incompleteTangleException.unavailableTransactionHash);
        }

        ict.submit(transaction1);
        saveSleep(50);
        boolean isTangleValid = validator.isTangleValid(transaction2.hash);
        Assert.assertTrue("Tangle was not recognized as valid after missing dependency was added.", isTangleValid);
    }

    @Test
    public void testStoreValidTransfers() {
        Ict ict = createIct();
        LedgerValidator validator = new LedgerValidator(ict);

        String transfer1 = buildRandomTransferAndPublish(ict, Collections.singleton(Transaction.NULL_TRANSACTION.hash));
        String transfer2 = buildRandomTransferAndPublish(ict, Collections.singleton(transfer1));

        boolean isTangleValid = validator.isTangleValid(transfer2, ict.findTransactionByHash(transfer2));

        Assert.assertTrue("Valid Tangle was recognized as invalid.", isTangleValid);
        Assert.assertTrue("Transfer was not added to valid transfer set.", validator.validTransfers.contains(transfer1));
        Assert.assertTrue("Transfer was not added to valid transfer set.", validator.validTransfers.contains(transfer2));
    }

    private static String buildRandomTransferAndPublish(Ict ict, Set<String> references) {
        Bundle randomTransfer = buildRandomTransfer(references);
        for(Transaction transaction : randomTransfer.getTransactions())
            ict.submit(transaction);
        return randomTransfer.getHead().hash;
    }

    private static Bundle buildRandomTransfer(Set<String> references) {
        Set<InputBuilder> inputBuilders = new HashSet<>();
        Set<OutputBuilder> outputBuilders = new HashSet<>();
        for(int i = 0; i < (references.size()+1)/2; i++)
            outputBuilders.add(randomOutput());
        TransferBuilder transferBuilder = new TransferBuilder(inputBuilders, outputBuilders, 1);
        BundleBuilder bundleBuilder = transferBuilder.build();

        List<TransactionBuilder> builders = bundleBuilder.getTailToHead();
        int i = 0;
        for(String reference : references) {
            TransactionBuilder builder = builders.get(i/2);
            if(i%2 == 0) {
                builder.branchHash = reference;
            } else {
                builder.trunkHash = reference;
            }
            i++;
        }

        return bundleBuilder.build();
    }

    private static OutputBuilder randomOutput() {
        return new OutputBuilder(Trytes.randomSequenceOfLength(Transaction.Field.ADDRESS.tryteLength), BigInteger.ZERO, Trytes.randomSequenceOfLength(Transaction.Field.SIGNATURE_FRAGMENTS.tryteLength));
    }
}