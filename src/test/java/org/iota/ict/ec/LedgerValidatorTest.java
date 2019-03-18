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
import org.iota.ict.utils.crypto.SignatureSchemeImplementation;
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
        boolean isTangleValid = validator.isTangleValid(transaction2.hash);
        Assert.assertTrue("Tangle was not recognized as valid after missing dependency was added.", isTangleValid);
    }

    @Test
    public void testStoreValidTransfers() {
        Ict ict = createIct();
        LedgerValidator validator = new LedgerValidator(ict);

        String transfer1 = buildRandomTransferAndSubmit(ict, Collections.singleton(Transaction.NULL_TRANSACTION.hash));
        String transfer2 = buildRandomTransferAndSubmit(ict, Collections.singleton(transfer1));

        boolean isTangleValid = validator.isTangleValid(transfer2);

        Assert.assertTrue("Valid Tangle was recognized as invalid.", isTangleValid);
        Assert.assertTrue("Transfer was not added to valid transfer set.", validator.validTransfers.contains(transfer1));
        Assert.assertTrue("Transfer was not added to valid transfer set.", validator.validTransfers.contains(transfer2));
    }

    @Test
    public void testDoubleSpend() {

        Ict ict = createIct();
        LedgerValidator validator = new LedgerValidator(ict);
        BigInteger value = BigInteger.valueOf(1000);

        SignatureSchemeImplementation.PrivateKey privateKey = SignatureSchemeImplementation.derivePrivateKeyFromSeed(Trytes.randomSequenceOfLength(81), 0, 1);
        validator.changeInitialBalance(privateKey.deriveAddress(), value);

        String spend1 = spendFunds(ict, privateKey, value, Trytes.randomSequenceOfLength(Transaction.Field.ADDRESS.tryteLength));
        String spend2 = spendFunds(ict, privateKey, value, Trytes.randomSequenceOfLength(Transaction.Field.ADDRESS.tryteLength));
        String doubleSpend = mergeTangles(ict, spend1, spend2);
        saveSleep(50);

        Assert.assertTrue("Solid Tangle was recognized as not solid.", validator.isTangleSolid(spend1));
        Assert.assertTrue("Solid Tangle was recognized as not solid.", validator.isTangleSolid(spend2));
        Assert.assertFalse("Double spend: funds of an address were spent twice.", validator.isTangleSolid(doubleSpend));
        Assert.assertFalse("Double spend: funds of an address were spent twice.", validator.areTanglesCompatible(spend1, spend2));

        validator.changeInitialBalance(privateKey.deriveAddress(), value);
        Assert.assertTrue("Double spend failed despite sufficient funds.", validator.isTangleSolid(doubleSpend));
        Assert.assertTrue("Double spend failed despite sufficient funds.", validator.areTanglesCompatible(spend1, spend2));
    }

    private static String mergeTangles(Ict ict, String branch, String trunk) {
        TransactionBuilder builder = new TransactionBuilder();
        builder.branchHash = branch;
        builder.trunkHash = trunk;
        Transaction merge = builder.build();
        ict.submit(merge);
        saveSleep(50);
        return merge.hash;
    }

    private static String spendFunds(Ict ict, SignatureSchemeImplementation.PrivateKey privateKey, BigInteger value, String receiverAddress) {
        InputBuilder inputBuilder = new InputBuilder(privateKey, BigInteger.ZERO.subtract(value));
        OutputBuilder outputBuilder = new OutputBuilder(receiverAddress, value, "HELLOWORLD");

        TransferBuilder transferBuilder = new TransferBuilder(Collections.singleton(inputBuilder), Collections.singleton(outputBuilder), 1);
        BundleBuilder bundleBuilder = transferBuilder.build();
        Bundle bundle = bundleBuilder.build();
        submitBundle(ict, bundle);

        return bundle.getHead().hash;
    }

    @Test
    public void testInvalidSignature() {
        Ict ict = createIct();
        LedgerValidator validator = new LedgerValidator(ict);
        String inputAddress = Trytes.randomSequenceOfLength(81);
        BigInteger value = BigInteger.valueOf(1000);
        validator.changeInitialBalance(inputAddress, value);

        Bundle bundleWithInvalidSignature = buildBundleWithInvalidSignature(inputAddress, value);
        submitBundle(ict, bundleWithInvalidSignature);

        boolean isTangleValid = validator.isTangleValid(bundleWithInvalidSignature.getHead().hash);
        Assert.assertFalse("Inalid Tangle was recognized as valid.", isTangleValid);
    }

    @Test
    public void testValidSignature() {
        Ict ict = createIct();
        LedgerValidator validator = new LedgerValidator(ict);
        BigInteger value = BigInteger.valueOf(1000);

        SignatureSchemeImplementation.PrivateKey privateKey = SignatureSchemeImplementation.derivePrivateKeyFromSeed(Trytes.randomSequenceOfLength(81), 0, 1);
        validator.changeInitialBalance(privateKey.deriveAddress(), value);

        String bundleOriginal = submitBundle(ict, buildValidTransfer(privateKey, value, privateKey.deriveAddress(), new HashSet<String>()));
        String bundleReattach = submitBundle(ict, buildValidTransfer(privateKey, value, privateKey.deriveAddress(), Collections.singleton(bundleOriginal)));

        Assert.assertTrue("Valid Tangle was recognized as invalid.", validator.isTangleValid(bundleReattach));
        Assert.assertTrue("Solid Tangle was recognized as not solid.", validator.isTangleSolid(bundleReattach));

        validator.changeInitialBalance(privateKey.deriveAddress(), BigInteger.ZERO.subtract(value.add(BigInteger.ONE)));
        Assert.assertFalse("Tangle was recognized as solid despite missing funds.", validator.isTangleSolid(bundleReattach));
    }


    @Test
    public void testSelfCompatibility() {
        Ict ict = createIct();
        LedgerValidator validator = new LedgerValidator(ict);
        BigInteger value = BigInteger.valueOf(1000);

        SignatureSchemeImplementation.PrivateKey privateKey = SignatureSchemeImplementation.derivePrivateKeyFromSeed(Trytes.randomSequenceOfLength(81), 0, 1);
        validator.changeInitialBalance(privateKey.deriveAddress(), value);

        String bundle = submitBundle(ict, buildValidTransfer(privateKey, value, privateKey.deriveAddress(), new HashSet<String>()));

        Assert.assertTrue("Valid Tangle was recognized as invalid.", validator.isTangleValid(bundle));
        Assert.assertTrue("Tangle is self-conflicting.", validator.areTanglesCompatible(bundle, bundle));
    }

    private static Bundle buildBundleWithInvalidSignature(String inputAddress, BigInteger value) {
        BundleBuilder bundleBuilder = new BundleBuilder();
        TransactionBuilder inputBuilder = new TransactionBuilder();
        TransactionBuilder outputBuilder = new TransactionBuilder();
        inputBuilder.address = inputAddress;
        outputBuilder.address = Trytes.randomSequenceOfLength(Transaction.Field.ADDRESS.tryteLength);
        inputBuilder.signatureFragments = Trytes.randomSequenceOfLength(Transaction.Field.SIGNATURE_FRAGMENTS.tryteLength);
        inputBuilder.value = BigInteger.ZERO.subtract(value);
        outputBuilder.value = value;
        bundleBuilder.append(inputBuilder);
        bundleBuilder.append(outputBuilder);
        return bundleBuilder.build();
    }

    private static String buildRandomTransferAndSubmit(Ict ict, Set<String> references) {
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