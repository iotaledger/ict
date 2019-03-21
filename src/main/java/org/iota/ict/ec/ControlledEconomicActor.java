package org.iota.ict.ec;

import org.iota.ict.model.bundle.*;
import org.iota.ict.model.transaction.Transaction;
import org.iota.ict.model.transfer.InputBuilder;
import org.iota.ict.model.transfer.OutputBuilder;
import org.iota.ict.model.transfer.TransferBuilder;
import org.iota.ict.model.transaction.TransactionBuilder;
import org.iota.ict.utils.Trytes;
import org.iota.ict.utils.crypto.AutoIndexedMerkleTree;
import org.iota.ict.utils.crypto.MerkleTree;
import org.iota.ict.utils.crypto.SignatureSchemeImplementation;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Allows to run an economic actor by issuing markers. The actively-writing counter-part to {@link TrustedEconomicActor}.
 * The actor can be followed by other nodes as reference for their economic cluster.
 * */
public class ControlledEconomicActor extends EconomicActor {

    protected final AutoIndexedMerkleTree merkleTree;

    public ControlledEconomicActor(AutoIndexedMerkleTree merkleTree) {
        super(merkleTree.getAddress());
        this.merkleTree = merkleTree;
    }

    public Bundle buildMarker(String trunk, String branch, double confidence) {

        Set<OutputBuilder> outputs = new HashSet<>();

        String messageToSign =  messageToSign(trunk, branch);
        SignatureSchemeImplementation.Signature signature = merkleTree.sign(messageToSign);
        assert signature.deriveAddress().equals(address);
        outputs.add(new OutputBuilder(address, BigInteger.ZERO, signature.toString()));

        TransferBuilder transferBuilder =  new TransferBuilder(new HashSet<InputBuilder>(), outputs, merkleTree.getSecurityLevel());
        BundleBuilder bundleBuilder = transferBuilder.build();

        List<TransactionBuilder> tailToHead = bundleBuilder.getTailToHead();
        tailToHead.get(0).branchHash = branch;
        tailToHead.get(0).trunkHash = trunk;
        tailToHead.get(0).tag = encodeConfidence(confidence, Transaction.Field.TAG.tryteLength);
        return bundleBuilder.build();
    }
}
