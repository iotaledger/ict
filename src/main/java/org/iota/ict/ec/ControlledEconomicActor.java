package org.iota.ict.ec;

import org.iota.ict.model.*;
import org.iota.ict.utils.Trytes;
import org.iota.ict.utils.crypto.SignatureScheme;

public class ControlledEconomicActor extends EconomicActor {

    private final int SIGNATURE_FRAGMENTS_TRYTE_LENGTH = Transaction.Field.SIGNATURE_FRAGMENTS.tryteLength;

    protected final String seed;

    public ControlledEconomicActor(String seed) {
        super(SignatureScheme.determineAddressFromSeed(seed, 0, 1));
        this.seed = seed;
    }

    public Bundle issueMarker(String trunk, String branch) {



        byte[] asciiSignature = privateKey.sign(messageToSign(trunk, branch));
        String signatureTrytes = Trytes.fromAscii(new String(asciiSignature));
        BundleBuilder markerBuilder = new BundleBuilder();

        assert signatureTrytes.length()%SIGNATURE_FRAGMENTS_TRYTE_LENGTH == 0;

        for(int i = 0; i < signatureTrytes.length(); i+=SIGNATURE_FRAGMENTS_TRYTE_LENGTH) {
            TransactionBuilder transactionBuilder = new TransactionBuilder();
            if(i == 0) {   transactionBuilder.trunkHash = trunk; } // tail transaction
            transactionBuilder.branchHash = branch;
            transactionBuilder.signatureFragments = signatureTrytes.substring(i, i+SIGNATURE_FRAGMENTS_TRYTE_LENGTH);
            transactionBuilder.address = getAddress();
            markerBuilder.append(transactionBuilder);
        }

        return markerBuilder.build();
    }
}
