package org.iota.ict.utils.crypto;

import org.iota.ict.utils.Trytes;
import org.junit.Assert;
import org.junit.Test;

public class SignatureSchemeTest {


    @Test
    public void testPublicKey() {

        String seed = Trytes.randomSequenceOfLength(81);
        String toSign = Trytes.randomSequenceOfLength(27);
        int securityLevel = 1;

        String privateKey = SignatureScheme.derivePrivateKeyFromSeed(seed, 0, securityLevel);
        String publicKey = SignatureScheme.derivePublicKeyFromPrivateKey(privateKey);

        String signature = SignatureScheme.sign(privateKey, toSign);
        String publicKeyOfSignature = SignatureScheme.derivePublicKeyFromSignature(signature, toSign);

        Assert.assertEquals("Public key of signature derived incorrectly.", publicKey, publicKeyOfSignature);
    }


    @Test
    public void testSignature() {

        String seed = Trytes.randomSequenceOfLength(81);
        int securityLevel = 1;

        String privateKey = SignatureScheme.derivePrivateKeyFromSeed(seed, 0, securityLevel);
        String addressOfPrivateKey = SignatureScheme.deriveAddressFromPrivateKey(privateKey);
        String toSign = Trytes.randomSequenceOfLength(27);

        String signature = SignatureScheme.sign(privateKey, toSign);
        String addressOfSignature = SignatureScheme.deriveAddressFromSignature(signature, toSign);

        Assert.assertEquals("Address of signature derived incorrectly.", addressOfPrivateKey, addressOfSignature);
    }
}