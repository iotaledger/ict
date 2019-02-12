package org.iota.ict.model.bundle;

import org.iota.ict.model.transaction.Transaction;
import org.iota.ict.utils.Trytes;
import org.iota.ict.utils.crypto.SignatureSchemeImplementation;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.util.*;

public class TransferTest {

    @Test
    public void testSecurityLevel() {

        String hashWithLevel3 = "999999999999999999999999999" + "999999999999999999999999999" + "999999999999999999999999999";
        String hashWithLevel2 = "999999999999999999999999999" + "DWDW99999999999999999999999" + "A99999999999999999999999999"; // D and W cancel out each-other
        String hashWithLevel1 = "999999999999999999999999999" + "B99999999999999999999999999" + "999999999999999999999999999";
        String hashWithLevel0 = "A99999999999999999999999999" + "B99999999999999999999999999" + "C99999999999999999999999999";

        Assert.assertEquals("failed to determine correct security level", 0, Transfer.calcSecurityLevel(hashWithLevel0));
        Assert.assertEquals("failed to determine correct security level", 1, Transfer.calcSecurityLevel(hashWithLevel1));
        Assert.assertEquals("failed to determine correct security level", 2, Transfer.calcSecurityLevel(hashWithLevel2));
        Assert.assertEquals("failed to determine correct security level", 3, Transfer.calcSecurityLevel(hashWithLevel3));
    }

    @Test
    public void testCollectInputsAndOutputs() {
        Set<InputBuilder> inputs = createRandomInputs(3, 1);
        BigInteger inputBalance = calcAvailableFunds(inputs);
        Set<BalanceChange> outputs = createRandomOutputs(inputBalance, 4);

        Bundle bundle = buildBundle(inputs, outputs);
        Transfer transfer = new Transfer(bundle);

        Assert.assertTrue("Signatures are invalid.", transfer.areSignaturesValid());
        Assert.assertEquals("Did not collect as many inputs as submitted transfer had.", inputs.size(), transfer.getInputs().size());
        Assert.assertEquals("Did not collect as many outputs as submitted transfer had.", outputs.size(), transfer.getOutputs().size());
        Assert.assertTrue("Some outputs of the submitted transfer were not collected.", transfer.getOutputs().containsAll(outputs));
        assertReferenceInputAreIncludedInTransfer(inputs, transfer);
    }

    private void assertReferenceInputAreIncludedInTransfer(Iterable<InputBuilder> referenceInputs, Transfer transfer) {
        // inputs must be treated differently than outputs because they now have a signature -> equal() does not work -> containsAll() does not work

        for (InputBuilder referenceInput : referenceInputs) {
            boolean foundReference = false;
            for (BalanceChange actualInput : transfer.getInputs())
                if (referenceInput.getValue().equals(actualInput.value) && referenceInput.getAddress().equals(actualInput.address))
                    foundReference = true;
            if (!foundReference)
                Assert.fail("An input is missing.");
        }
    }

    private Bundle buildBundle(Set<InputBuilder> inputs, Set<BalanceChange> outputs) {
        TransferBuilder transferBuilder = new TransferBuilder(inputs, outputs, 1);
        BundleBuilder bundleBuilder = transferBuilder.build();
        return bundleBuilder.build();
    }

    private <T extends BalanceChangeBuilderModel> BigInteger calcAvailableFunds(Set<T> changes) {
        BigInteger availableFunds = BigInteger.ZERO;
        for (BalanceChangeBuilderModel change : changes)
            availableFunds = availableFunds.subtract(change.getValue());
        return availableFunds;
    }

    private Set<InputBuilder> createRandomInputs(int amount, int securityLevel) {
        assert amount > 0;
        Set<InputBuilder> inputs = new HashSet<>();
        for (int i = 0; i < amount; i++)
            inputs.add(createRandomInput(securityLevel));
        return inputs;
    }

    private Set<BalanceChange> createRandomOutputs(BigInteger availableFunds, int amount) {
        assert amount > 0;
        Set<BalanceChange> outputs = new HashSet<>();
        for (int i = 0; i < amount - 1; i++) {
            BigInteger value = availableFunds.multiply(BigInteger.valueOf((long) (Math.random() * 10000))).divide(BigInteger.valueOf(10000));
            outputs.add(createRandomOutput(value));
            availableFunds = availableFunds.subtract(value);
        }
        outputs.add(createRandomOutput(availableFunds));
        return outputs;
    }

    private static InputBuilder createRandomInput(int securityLevel) {
        BigInteger value = BigInteger.valueOf((long) (Math.random() * Long.MIN_VALUE)).subtract(BigInteger.ONE);
        String seed = Trytes.randomSequenceOfLength(Transaction.Field.ADDRESS.tryteLength);
        SignatureSchemeImplementation.PrivateKey privateKey = SignatureSchemeImplementation.derivePrivateKeyFromSeed(seed, 0, securityLevel);
        return new InputBuilder(privateKey, value, securityLevel);
    }

    private static BalanceChange createRandomOutput(BigInteger value) {
        assert value.compareTo(BigInteger.ZERO) >= 0;
        String address = Trytes.randomSequenceOfLength(Transaction.Field.ADDRESS.tryteLength);
        String message = Trytes.randomSequenceOfLength((int)(Math.random() * 3+1) * Transaction.Field.SIGNATURE_FRAGMENTS.tryteLength);
        return new BalanceChange(address, value, message);
    }
}
