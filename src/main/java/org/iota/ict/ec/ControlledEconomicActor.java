package org.iota.ict.ec;

import org.iota.ict.model.*;
import org.iota.ict.utils.crypto.SignatureScheme;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ControlledEconomicActor extends EconomicActor {

    protected final String seed;
    protected final SignatureScheme.PrivateKey privateKey;

    public ControlledEconomicActor(String seed) {
        super(SignatureScheme.deriveAddressFromSeed(seed, 0, 3));
        privateKey = SignatureScheme.derivePrivateKeyFromSeed(seed, 0, 3);
        this.seed = seed;
    }

    public Bundle issueMarker(String trunk, String branch, int securityLevel) {

        Set<BalanceChange> outputs = new HashSet<>();

        String messageToSign =  messageToSign(trunk, branch);
        String signature = privateKey.sign(messageToSign);
        outputs.add(new BalanceChange(address, BigInteger.ZERO, signature));

        TransferBuilder transferBuilder =  new TransferBuilder(new HashSet<InputBuilder>(), outputs, securityLevel);
        BundleBuilder bundleBuilder = transferBuilder.build();

        List<TransactionBuilder> tailToHead = bundleBuilder.getTailToHead();
        tailToHead.get(0).branchHash = branch;
        tailToHead.get(0).trunkHash = trunk;
        return bundleBuilder.build();
    }
}
