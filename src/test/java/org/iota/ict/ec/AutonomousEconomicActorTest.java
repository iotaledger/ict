package org.iota.ict.ec;

import org.iota.ict.Ict;
import org.iota.ict.IctTestTemplate;
import org.iota.ict.utils.Trytes;
import org.iota.ict.utils.crypto.MerkleTree;
import org.iota.ict.utils.crypto.SignatureSchemeImplementation;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.util.Collections;

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

        String transfer1 = submitBundle(ict, buildValidTransfer(key1, value, key2.deriveAddress(), Collections.<String>emptySet()));
        String transfer2 = submitBundle(ict, buildValidTransfer(key2, value, key1.deriveAddress(), Collections.singleton(transfer1)));

        Assert.assertEquals(0, cluster.determineApprovalConfidence(transfer1), 1E-3);
        Assert.assertEquals(0, cluster.determineApprovalConfidence(transfer2), 1E-3);

        submitBundle(ict, otherA.buildMarker(transfer1, transfer1, 0.5));
        saveSleep(50);
        Assert.assertEquals(0.3*0.5, cluster.determineApprovalConfidence(transfer1), 1E-3);

        // transfer1 is not valid
        underTest.tick();
        saveSleep(100);
        Assert.assertEquals(0.5*0 + 0.3*0.5, cluster.determineApprovalConfidence(transfer1), 1E-2);
        Assert.assertEquals(0, cluster.determineApprovalConfidence(transfer2), 1E-2);


        submitBundle(ict, otherB.buildMarker(transfer2, transfer2, 0.7));
        saveSleep(50);
        Assert.assertEquals(0.2*0.7, cluster.determineApprovalConfidence(transfer2), 1E-2);

        underTest.tick();
        saveSleep(100);
        Assert.assertEquals(0.5*1 + 0.3*0.5 + 0.2*0.7, cluster.determineApprovalConfidence(transfer1), 1E-2);
        Assert.assertEquals(0.5*1 + 0.2*0.7, cluster.determineApprovalConfidence(transfer2), 1E-2);
    }

    private MerkleTree randomMerkleTree() {
        return new MerkleTree(Trytes.randomSequenceOfLength(81), 3, 3);
    }
}