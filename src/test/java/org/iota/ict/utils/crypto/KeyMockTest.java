package org.iota.ict.utils.crypto;

import org.iota.ict.utils.Trytes;
import org.junit.Assert;
import org.junit.Test;

public class KeyMockTest {

    @Test
    public void testEncryptionDecryption() {
        for(int testIteration = 0; testIteration < 100; testIteration++) {
            KeyPair keyPair = new KeyPairMock();
            String original = randomMessage();
            String decrypted = new String(keyPair.getPrivateKey().decrypt(keyPair.getPublicKey().encrypt(original.getBytes())));
            Assert.assertEquals("Key mock encryption-decryption does not work.", original, decrypted);
        }
    }

    @Test
    public void testEncryptionDecryptionWithWrongKey() {
        for(int testIteration = 0; testIteration < 100; testIteration++) {
            PrivateKey privateKey = new KeyPairMock().getPrivateKey();
            PublicKey publicKey = new KeyPairMock().getPublicKey();
            String original = randomMessage();
            String decrypted = new String(privateKey.decrypt(publicKey.encrypt(original.getBytes())));
            Assert.assertNotEquals("Key mock encryption-decryption does work with wrong key.", original, decrypted);
        }
    }

    @Test
    public void testSignatureValidation() {
        for(int testIteration = 0; testIteration < 100; testIteration++) {
            KeyPair keyPair = new KeyPairMock();
            byte[] message = randomMessage().getBytes();
            byte[] signature = keyPair.getPrivateKey().sign(message);
            Assert.assertTrue("Key mock signature does not work.", keyPair.getPublicKey().verifySignature(signature, message));
        }
    }

    @Test
    public void testSignatureValidationWithWrongKey() {
        for(int testIteration = 0; testIteration < 100; testIteration++) {
            PrivateKey privateKey = new KeyPairMock().getPrivateKey();
            PublicKey publicKey = new KeyPairMock().getPublicKey();
            byte[] message = randomMessage().getBytes();
            byte[] signature = privateKey.sign(message);
            Assert.assertFalse("Key mock signature does work with wrong key.", publicKey.verifySignature(signature, message));
        }
    }

    private static String randomMessage() {
        return  Trytes.randomSequenceOfLength((int)(Math.random()*100)) + "dt/(§!)";
    }
}
