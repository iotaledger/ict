package org.iota.ict.utils.crypto;

import org.iota.ict.utils.Trytes;
import org.junit.Assert;
import org.junit.Test;

public class SignatureSchemeTest {


    @Test
    public void testPublicKey() {

        String seed = Trytes.randomSequenceOfLength(81);
        int securityLevel = (int)(Math.random() * 3)+1;
        String toSign = Trytes.randomSequenceOfLength(27 * securityLevel);

        SignatureScheme.PrivateKey privateKey = SignatureScheme.derivePrivateKeyFromSeed(seed, 0, securityLevel);
        SignatureScheme.PublicKey publicKey = privateKey.derivePublicKey();

        String signature = privateKey.sign(toSign);
        SignatureScheme.PublicKey publicKeyOfSignature = SignatureScheme.derivePublicKeyFromSignature(signature, toSign);

        Assert.assertEquals("Public key of signature derived incorrectly.", publicKey, publicKeyOfSignature);
    }


    @Test
    public void testSignature() {

        String seed = Trytes.randomSequenceOfLength(81);
        int securityLevel = (int)(Math.random() * 3)+1;

        SignatureScheme.PrivateKey privateKey = SignatureScheme.derivePrivateKeyFromSeed(seed, 0, securityLevel);
        String addressOfPrivateKey = privateKey.deriveAddress();
        String toSign = Trytes.randomSequenceOfLength(27 * securityLevel);

        String signature = privateKey.sign(toSign);
        String addressOfSignature = SignatureScheme.deriveAddressFromSignature(signature, toSign);

        Assert.assertEquals("Address of signature derived incorrectly.", addressOfPrivateKey, addressOfSignature);
    }
}