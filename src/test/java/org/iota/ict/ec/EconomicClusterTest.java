package org.iota.ict.ec;

import org.iota.ict.Ict;
import org.iota.ict.IctTestTemplate;
import org.iota.ict.model.bundle.Bundle;
import org.iota.ict.utils.Trytes;
import org.iota.ict.model.transaction.Transaction;
import org.iota.ict.model.transaction.TransactionBuilder;
import org.iota.ict.utils.crypto.MerkleTree;
import org.junit.Assert;
import org.junit.Test;

public class EconomicClusterTest extends IctTestTemplate {

    @Test
    public void testOverwriteConfidence() {
        Ict ict = createIct();
        ControlledEconomicActor ca = new ControlledEconomicActor(new MerkleTree(Trytes.randomSequenceOfLength(81), 3,3), 0);
        EconomicCluster cluster = new EconomicCluster(ict);
        TrustedEconomicActor ta = new TrustedEconomicActor(ca.getAddress(),0.2);
        cluster.addActor(ta);

        Transaction transaction = new TransactionBuilder().build();
        ict.submit(transaction);

        // this marker will be created first to ensure the index is zero
        Bundle markerWithIndex0 = ca.issueMarker(transaction.hash, transaction.hash, 1, 3/26.0);

        assertApprovalRate(cluster, transaction, 0);
        sendMarker(ict, ca, transaction.hash, 22/26.0);
        assertApprovalRate(cluster, transaction, 22/26.0);
        sendMarker(ict, ca, transaction.hash, 17/26.0);
        assertApprovalRate(cluster, transaction, 17/26.0);

        // issue marker with index zero: approval rate should not change because marker is old
        sendMarker(ict, markerWithIndex0);
        assertApprovalRate(cluster, transaction, 17/26.0);
    }

    @Test
    public void testReferencedByMultipleMarkers() {
        Ict ict = createIct();
        ControlledEconomicActor ca = new ControlledEconomicActor(new MerkleTree(Trytes.randomSequenceOfLength(81), 3,3), 0);
        EconomicCluster cluster = new EconomicCluster(ict);
        TrustedEconomicActor ta = new TrustedEconomicActor(ca.getAddress(),1);
        cluster.addActor(ta);

        Transaction transaction = new TransactionBuilder().build();
        ict.submit(transaction);

        Transaction childA = buildChild(transaction);
        Transaction childB = buildChild(transaction);
        ict.submit(childA);
        ict.submit(childB);

        double markerAConfidence = 14/26.0;
        double markerBConfidence = 17/26.0;

        sendMarker(ict, ca, childB.hash, markerBConfidence);
        sendMarker(ict, ca, childA.hash, markerAConfidence);
        assertApprovalRate(cluster, transaction, Math.max(markerAConfidence, markerBConfidence));

        markerBConfidence = 11/26.0;
        sendMarker(ict, ca, childB.hash, markerBConfidence);
        assertApprovalRate(cluster, transaction, Math.max(markerAConfidence, markerBConfidence));
    }

    private Transaction buildChild(Transaction parent) {
        TransactionBuilder childBuilder = new TransactionBuilder();
        childBuilder.branchHash = parent.hash;
        return childBuilder.build();
    }

    @Test
    public void testDetermineApprovalConfidence() {

        Ict ict = createIct();

        ControlledEconomicActor ca1 = new ControlledEconomicActor(new MerkleTree(Trytes.randomSequenceOfLength(81), 3,3), 0);
        ControlledEconomicActor ca2 = new ControlledEconomicActor(new MerkleTree(Trytes.randomSequenceOfLength(81), 3, 3), 0);
        ControlledEconomicActor ca3 = new ControlledEconomicActor(new MerkleTree(Trytes.randomSequenceOfLength(81), 3, 3), 0);

        EconomicCluster cluster = new EconomicCluster(ict);
        TrustedEconomicActor ta1 = new TrustedEconomicActor(ca1.getAddress(),0.2);
        TrustedEconomicActor ta2 = new TrustedEconomicActor(ca2.getAddress(),0.3);
        TrustedEconomicActor ta3 = new TrustedEconomicActor(ca3.getAddress(),0.4);
        cluster.addActor(ta1);
        cluster.addActor(ta2);
        cluster.addActor(ta3);

        double maxTrust = ta1.getTrust() + ta2.getTrust() + ta3.getTrust();

        Transaction transaction = new TransactionBuilder().build();
        ict.submit(transaction);
        saveSleep(50);

        assertApprovalRate(cluster, transaction, 0);

        sendMarker(ict, ca1, transaction.hash, 1);
        assertApprovalRate(cluster, transaction, ta1.getTrust()/maxTrust);

        // do it twice: being referenced twice by the same actor shouldn't change the approval rate
        for(int i = 0; i < 2; i++) {
            sendMarker(ict, ca2, transaction.hash, 1);
            assertApprovalRate(cluster, transaction,  (ta1.getTrust() + ta2.getTrust()) / maxTrust);
        }
    }

    @Test
    public void testIctCommunication() {

        Ict ictA = createIct();
        Ict ictB = createIct();
        connect(ictA, ictB);

        ControlledEconomicActor ca = new ControlledEconomicActor(new MerkleTree(Trytes.randomSequenceOfLength(81), 3,3), 0);

        EconomicCluster cluster = new EconomicCluster(ictB);
        TrustedEconomicActor ta = new TrustedEconomicActor(ca.getAddress(),0.2);
        cluster.addActor(ta);

        Transaction transaction = new TransactionBuilder().build();
        ictA.submit(transaction);
        waitUntilCommunicationEnds(50);

        assertApprovalRate(cluster, transaction, 0);

        sendMarker(ictA, ca, transaction.hash, 1);
        assertApprovalRate(cluster, transaction, 1);
    }

    private void sendMarker(Ict ict, ControlledEconomicActor actor, String referencedHash, double confidence) {
        Bundle marker = actor.issueMarker(referencedHash, referencedHash, 1, confidence);
        sendMarker(ict, marker);
    }

    private void sendMarker(Ict ict, Bundle marker) {
        for(Transaction markerTransaction : marker.getTransactions())
            ict.submit(markerTransaction);
        waitUntilCommunicationEnds(100);
        saveSleep(100);
    }

    private void assertApprovalRate(EconomicCluster cluster, Transaction transaction, double expected) {
        double approvalConfidence = cluster.determineApprovalConfidence(transaction.hash);
        Assert.assertEquals("Unexpected transaction approval confidence for "+transaction.hash+".", expected, approvalConfidence, 1E-10);
    }
}