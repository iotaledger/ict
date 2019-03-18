package org.iota.ict;

import org.iota.ict.model.bundle.Bundle;
import org.iota.ict.model.bundle.BundleBuilder;
import org.iota.ict.model.transaction.Transaction;
import org.iota.ict.model.transaction.TransactionBuilder;
import org.iota.ict.model.transfer.InputBuilder;
import org.iota.ict.model.transfer.OutputBuilder;
import org.iota.ict.model.transfer.TransferBuilder;
import org.iota.ict.utils.Trytes;
import org.iota.ict.utils.crypto.SignatureSchemeImplementation;
import org.iota.ict.utils.properties.EditableProperties;
import org.junit.After;

import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class IctTestTemplate {

    protected Set<Ict> runningIcts = new HashSet<>();
    private final static int DEFAULT_PORT = 1337;

    protected Ict createIct() {
        return createIct(new EditableProperties());
    }

    protected Ict createIct(EditableProperties properties) {
        properties.host("localhost")
                .port(DEFAULT_PORT + runningIcts.size())
                .minForwardDelay(0)
                .maxForwardDelay(10)
                .guiEnabled(false);
        Ict ict = new Ict(properties.toFinal());
        runningIcts.add(ict);
        return ict;
    }

    @After
    public void tearDown() {
        for (Ict ict : runningIcts)
            ict.terminate();
        runningIcts = new HashSet<>();
    }

    protected static void saveSleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
        }
    }

    protected static void connect(Ict ict1, Ict ict2) {
        addNeighborToIct(ict1, ict2);
        addNeighborToIct(ict2, ict1);
    }

    private static void addNeighborToIct(Ict ict, Ict neighbor) {
        EditableProperties properties = ict.getProperties().toEditable();
        Set<String> neighbors = properties.neighbors();
        neighbors.add(neighbor.getAddress().getHostName() + ":" + neighbor.getAddress().getPort());
        properties.neighbors(neighbors);
        ict.updateProperties(properties.toFinal());
    }

    private boolean anyIctStillCommunicating() {
        for (Ict ict : runningIcts)
            if(ict.node.getSenderQueueSize() > 0)
                return true;
        return false;
    }

    protected void waitUntilCommunicationEnds(long maxWaitTime) {

        long waitingSince = System.currentTimeMillis();

        do {
            saveSleep(10);
        } while (anyIctStillCommunicating() && System.currentTimeMillis() - waitingSince < maxWaitTime);

        saveSleep(10);
    }

    protected static Bundle buildValidTransfer(SignatureSchemeImplementation.PrivateKey privateKey, BigInteger value, String receiverAddress, Set<String> references) {

        InputBuilder inputBuilder = new InputBuilder(privateKey, BigInteger.ZERO.subtract(value));
        Set<OutputBuilder> outputs = new HashSet<>();
        outputs.add(new OutputBuilder(receiverAddress, value, ""));
        for(int i = 0; i < references.size() - 3; i++)
            outputs.add(new OutputBuilder(Trytes.randomSequenceOfLength(81), BigInteger.ZERO, ""));

        TransferBuilder transferBuilder = new TransferBuilder(Collections.singleton(inputBuilder), outputs, 1);
        BundleBuilder bundleBuilder = transferBuilder.build();

        // apply references
        List<TransactionBuilder> tailToHead = bundleBuilder.getTailToHead();
        int i = -1;
        for(String reference : references) {
            if(i == -1)
                tailToHead.get(0).trunkHash = reference;
            else tailToHead.get(i).branchHash = reference;
            i++;
        }

        return bundleBuilder.build();
    }

    protected static String submitBundle(Ict ict, Bundle bundle) {
        for(Transaction transaction : bundle.getTransactions())
            ict.submit(transaction);
        saveSleep(50);
        return bundle.getHead().hash;
    }
}
