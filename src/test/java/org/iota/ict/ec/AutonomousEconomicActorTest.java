package org.iota.ict.ec;

import org.iota.ict.Ict;
import org.iota.ict.IctTestTemplate;
import org.iota.ict.utils.Trytes;
import org.iota.ict.utils.crypto.MerkleTree;
import org.iota.ict.utils.crypto.SignatureSchemeImplementation;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.util.HashSet;

public class AutonomousEconomicActorTest extends IctTestTemplate {

    @Test
    public void test() {

        Ict ict = createIct();
        EconomicCluster cluster = new EconomicCluster(ict);

        AutonomousEconomicActor underTest = new AutonomousEconomicActor(ict, cluster, randomMerkleTree(),0);
        ControlledEconomicActor otherA = new ControlledEconomicActor(randomMerkleTree(), 0);
        ControlledEconomicActor otherB = new ControlledEconomicActor(randomMerkleTree(), 0);

        cluster.addActor(new TrustedEconomicActor(underTest.getAddress(), 0.5));
        cluster.addActor(new TrustedEconomicActor(otherA.getAddress(), 0.3));
        cluster.addActor(new TrustedEconomicActor(otherB.getAddress(), 0.2));

        SignatureSchemeImplementation.PrivateKey key1 = SignatureSchemeImplementation.derivePrivateKeyFromSeed(Trytes.randomSequenceOfLength(81), 0, 1);
        SignatureSchemeImplementation.PrivateKey key2 = SignatureSchemeImplementation.derivePrivateKeyFromSeed(Trytes.randomSequenceOfLength(81), 0, 1);
        BigInteger value = BigInteger.valueOf(10);

        String transfer1 = submitBundle(ict, buildValidTransfer(key1, value, key2.deriveAddress(), new HashSet<String>()));

        Assert.assertEquals(0, cluster.determineApprovalConfidence(transfer1), 1E-3);

        submitBundle(ict, otherA.buildMarker(transfer1, transfer1, 0.5));
        saveSleep(50);
        Assert.assertEquals(0.5 * 0.3, cluster.determineApprovalConfidence(transfer1), 1E-3);

        underTest.tick();
        saveSleep(100);
        Assert.assertEquals(0.5 * 1 + 0.5 * 0.3, cluster.determineApprovalConfidence(transfer1), 1E-3);
    }

    private MerkleTree randomMerkleTree() {
        return new MerkleTree(Trytes.randomSequenceOfLength(81), 3, 3);
    }
}