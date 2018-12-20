package org.iota.ict.model;

import org.iota.ict.utils.Trytes;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.util.*;

public class TransferTest {

    @Test
    public void testCollectInputs() {
        Set<BalanceChange> inputs = createRandomInputs(3);
        BigInteger inputBalance = calcAvailableFunds(inputs);
        Set<BalanceChange> outputs = createRandomOutputs(inputBalance, 4);

        Bundle bundle = buildBundle(inputs, outputs);
        List<BalanceChange> collectedInputs = bundle.collectInputs();

        Assert.assertEquals("Did not collect as many inputs as submitted transfer had.", inputs.size(), collectedInputs.size());
        Assert.assertTrue("Some inputs of the submitted transfer were not collected.", collectedInputs.containsAll(inputs));
    }

    private Bundle buildBundle(Set<BalanceChange> inputs, Set<BalanceChange> outputs) {
        List<BalanceChange> allChanges = new LinkedList<>();
        allChanges.addAll(inputs);
        allChanges.addAll(outputs);
        Collections.shuffle(allChanges);
        Transfer transfer = new Transfer(allChanges);
        return transfer.buildBundle();
    }

    private BigInteger calcAvailableFunds(Set<BalanceChange> changes) {
        BigInteger availableFunds = BigInteger.ZERO;
        for(BalanceChange change : changes)
            availableFunds = availableFunds.subtract(change.value);
        return availableFunds;
    }

    private Set<BalanceChange> createRandomInputs(int amount) {
        assert amount > 0;
        Set<BalanceChange> inputs = new HashSet<>();
        for(int i = 0; i < amount; i++)
            inputs.add(createRandomInput());
        return inputs;
    }

    private Set<BalanceChange> createRandomOutputs(BigInteger availableFunds, int amount) {
        assert amount > 0;
        Set<BalanceChange> outputs = new HashSet<>();
        for(int i = 0; i < amount-1; i++) {
            BigInteger value = availableFunds.multiply(BigInteger.valueOf((long)(Math.random() * 10000))).divide(BigInteger.valueOf(10000));
            outputs.add(createRandomBalanceChange(value));
            availableFunds = availableFunds.subtract(value);
        }
        outputs.add(createRandomBalanceChange(availableFunds));
        return outputs;
    }

    private static BalanceChange createRandomInput() {
        BigInteger value = BigInteger.valueOf((long)(Math.random()*Long.MIN_VALUE));
        return createRandomBalanceChange(value);
    }

    private static BalanceChange createRandomBalanceChange(BigInteger value) {
        String address = Trytes.randomSequenceOfLength(Transaction.Field.ADDRESS.tryteLength);
        String message =  Trytes.randomSequenceOfLength(2 * Transaction.Field.SIGNATURE_FRAGMENTS.tryteLength);
        return new BalanceChange(address, value, message);
    }
}
