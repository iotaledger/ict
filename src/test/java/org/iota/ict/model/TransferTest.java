package org.iota.ict.model;

import org.iota.ict.utils.Trytes;
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
        Set<BalanceChange> inputs = createRandomInputs(3);
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

    private void assertReferenceInputAreIncludedInTransfer(Iterable<BalanceChange> referenceInputs, Transfer transfer) {
        // inputs must be treated differently than outputs because they now have a signature -> equal() does not work -> containsAll() does not work

        for (BalanceChange referenceInput : referenceInputs) {
            boolean foundReference = false;
            for (BalanceChange actualInput : transfer.getInputs())
                if (referenceInput.value.equals(actualInput.value) && referenceInput.address.equals(actualInput.address))
                    foundReference = true;
            if (!foundReference)
                Assert.fail("An input is missing.");
        }
    }

    private Bundle buildBundle(Set<BalanceChange> inputs, Set<BalanceChange> outputs) {
        List<BalanceChange> allChanges = new LinkedList<>();
        allChanges.addAll(inputs);
        allChanges.addAll(outputs);
        Collections.shuffle(allChanges);
        return TransferBuilder.buildBundle(new HashSet<>(allChanges), 1);
    }

    private BigInteger calcAvailableFunds(Set<BalanceChange> changes) {
        BigInteger availableFunds = BigInteger.ZERO;
        for (BalanceChange change : changes)
            availableFunds = availableFunds.subtract(change.value);
        return availableFunds;
    }

    private Set<BalanceChange> createRandomInputs(int amount) {
        assert amount > 0;
        Set<BalanceChange> inputs = new HashSet<>();
        for (int i = 0; i < amount; i++)
            inputs.add(createRandomInput());
        return inputs;
    }

    private Set<BalanceChange> createRandomOutputs(BigInteger availableFunds, int amount) {
        assert amount > 0;
        Set<BalanceChange> outputs = new HashSet<>();
        for (int i = 0; i < amount - 1; i++) {
            BigInteger value = availableFunds.multiply(BigInteger.valueOf((long) (Math.random() * 10000))).divide(BigInteger.valueOf(10000));
            outputs.add(createRandomBalanceChange(value));
            availableFunds = availableFunds.subtract(value);
        }
        outputs.add(createRandomBalanceChange(availableFunds));
        return outputs;
    }

    private static BalanceChange createRandomInput() {
        BigInteger value = BigInteger.valueOf((long) (Math.random() * Long.MIN_VALUE));
        return createRandomBalanceChange(value);
    }

    private static BalanceChange createRandomBalanceChange(BigInteger value) {
        String address = Trytes.randomSequenceOfLength(Transaction.Field.ADDRESS.tryteLength);
        String message = Trytes.randomSequenceOfLength(2 * Transaction.Field.SIGNATURE_FRAGMENTS.tryteLength);
        return new BalanceChange(address, value, message);
    }
}
