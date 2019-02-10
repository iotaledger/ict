package org.iota.ict.utils.crypto;

import org.iota.ict.utils.Trytes;
import org.junit.Assert;
import org.junit.Test;

public class SignatureSchemeImplementationTest {


    @Test
    public void testPublicKey() {

        String seed = Trytes.randomSequenceOfLength(81);
        int securityLevel = (int)(Math.random() * 3)+1;
        String toSign = Trytes.randomSequenceOfLength(27 * securityLevel);

        SignatureSchemeImplementation.PrivateKey privateKey = SignatureSchemeImplementation.derivePrivateKeyFromSeed(seed, 0, securityLevel);
        SignatureSchemeImplementation.PublicKey publicKey = privateKey.derivePublicKey();

        SignatureSchemeImplementation.Signature signature = privateKey.sign(toSign);
        Assert.assertEquals("Public key of signature derived incorrectly.", publicKey, signature.derivePublicKey());
    }


    @Test
    public void testSignature() {

        String seed = Trytes.randomSequenceOfLength(81);
        int securityLevel = (int)(Math.random() * 3)+1;

        SignatureSchemeImplementation.PrivateKey privateKey = SignatureSchemeImplementation.derivePrivateKeyFromSeed(seed, 0, securityLevel);
        String addressOfPrivateKey = privateKey.deriveAddress();
        String toSign = Trytes.randomSequenceOfLength(27 * securityLevel);

        SignatureSchemeImplementation.Signature signature = privateKey.sign(toSign);
        Assert.assertEquals("Address of signature derived incorrectly.", addressOfPrivateKey, signature.deriveAddress());
    }
}