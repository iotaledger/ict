package org.iota.ict.ec;

import org.iota.ict.Ict;
import org.iota.ict.IctTestTemplate;
import org.iota.ict.utils.Trytes;
import org.iota.ict.utils.crypto.AutoIndexedMerkleTree;
import org.iota.ict.utils.crypto.MerkleTree;
import org.iota.ict.utils.crypto.SignatureSchemeImplementation;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class AutonomousEconomicActorTest extends IctTestTemplate {

    @Test
    public void test() {

        Ict ict = createIct();
        EconomicCluster cluster = new EconomicCluster(ict);

        AutonomousEconomicActor underTest = new AutonomousEconomicActor(ict, cluster, new HashMap<String, BigInteger>(), randomMerkleTree());
        ControlledEconomicActor otherA = new ControlledEconomicActor(randomMerkleTree());
        ControlledEconomicActor otherB = new ControlledEconomicActor(randomMerkleTree());

        cluster.addActor(new TrustedEconomicActor(underTest.getAddress(), 0.5));
        cluster.addActor(new TrustedEconomicActor(otherA.getAddress(), 0.3));
        cluster.addActor(new TrustedEconomicActor(otherB.getAddress(), 0.2));

        SignatureSchemeImplementation.PrivateKey key1 = SignatureSchemeImplementation.derivePrivateKeyFromSeed(Trytes.randomSequenceOfLength(81), 0, 1);
        SignatureSchemeImplementation.PrivateKey key2 = SignatureSchemeImplementation.derivePrivateKeyFromSeed(Trytes.randomSequenceOfLength(81), 0, 1);
        BigInteger value = BigInteger.valueOf(10);

        String transfer1 = submitBundle(ict, buildValidTransfer(key1, value, key2.deriveAddress(), Collections.<String>emptySet()));
        String transfer2 = submitBundle(ict, buildValidTransfer(key2, value, key1.deriveAddress(), Collections.singleton(transfer1)));

        assertConfidence(cluster, transfer1, 0);
        assertConfidence(cluster, transfer2, 0);

        submitBundle(ict, otherA.buildMarker(transfer1, transfer1, 0.5));
        saveSleep(50);

        assertConfidence(cluster, transfer1, 0.3*0.5);

        // transfer1 is not valid
        underTest.tick();
        saveSleep(100);
        assertConfidence(cluster, transfer1, 0.5*0 + 0.3*0.5);


        submitBundle(ict, otherB.buildMarker(transfer2, transfer2, 0.7));
        saveSleep(50);
        assertConfidence(cluster, transfer2, 0.2*0.7);

        underTest.tick();
        saveSleep(100);
        assertConfidence(cluster, transfer1, 0.5*1 + 0.3*0.5 + 0.2*0.7);
        assertConfidence(cluster, transfer2, 0.5*1 + 0.2*0.7);
    }

    @Test
    public void testConvergence() {

        Ict ict = createIct();
        EconomicCluster cluster = new EconomicCluster(ict);

        SignatureSchemeImplementation.PrivateKey key = SignatureSchemeImplementation.derivePrivateKeyFromSeed(Trytes.randomSequenceOfLength(81), 0, 1);
        BigInteger value = BigInteger.valueOf(10);

        Map<String, BigInteger> initialBalances = new HashMap<>();
        initialBalances.put(key.deriveAddress(), value);

        AutonomousEconomicActor auto1 = new AutonomousEconomicActor(ict, cluster, initialBalances, randomMerkleTree());
        AutonomousEconomicActor auto2 = new AutonomousEconomicActor(ict, cluster, initialBalances, randomMerkleTree());
        AutonomousEconomicActor auto3 = new AutonomousEconomicActor(ict, cluster, initialBalances, randomMerkleTree());

        cluster.addActor(new TrustedEconomicActor(auto1.getAddress(), 0.5));
        cluster.addActor(new TrustedEconomicActor(auto2.getAddress(), 0.3));
        cluster.addActor(new TrustedEconomicActor(auto3.getAddress(), 0.2));

        String transfer1 = submitBundle(ict, buildValidTransfer(key, value, Trytes.randomSequenceOfLength(81), Collections.<String>emptySet()));
        String transfer2 = submitBundle(ict, buildValidTransfer(key, value, Trytes.randomSequenceOfLength(81), Collections.<String>emptySet()));

        assertConfidence(cluster, transfer1, 0);
        assertConfidence(cluster, transfer2, 0);

        Assert.assertEquals(0, cluster.determineApprovalConfidence(transfer1), 1E-2);
        Assert.assertEquals(0, cluster.determineApprovalConfidence(transfer2), 1E-2);

        submitBundle(ict, auto2.buildMarker(transfer1, transfer1, 0.5));
        submitBundle(ict, auto3.buildMarker(transfer2, transfer2, 0.5));
        saveSleep(50);

        assertConfidence(cluster, transfer1, 0.3*0.5);
        assertConfidence(cluster, transfer2, 0.2*0.5);

        for(int iteration = 0; iteration < 10; iteration++) {
            auto1.tick();
            auto2.tick();
            auto3.tick();
            saveSleep(100);
        }

        assertConfidence(cluster, transfer1, 1);
        assertConfidence(cluster, transfer2, 0);
    }

    private static void assertConfidence(EconomicCluster cluster, String transaction, double expected) {
        Assert.assertEquals("Unexpected confidence of " + transaction, expected, cluster.determineApprovalConfidence(transaction), 1E-2);
    }

    private AutoIndexedMerkleTree randomMerkleTree() {
        return new AutoIndexedMerkleTree(Trytes.randomSequenceOfLength(81), 3, 3);
    }
}