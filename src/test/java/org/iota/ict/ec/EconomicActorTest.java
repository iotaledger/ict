package org.iota.ict.ec;

import org.iota.ict.utils.crypto.KeyPairMock;
import org.iota.ict.model.Transaction;
import org.iota.ict.utils.Trytes;
import org.iota.ict.utils.crypto.PublicKey;
import org.junit.Assert;
import org.junit.Test;

public class EconomicActorTest {

    @Test
    public void testAddressDerivation() {
        for(int testIteration = 0; testIteration < 100; testIteration++) {
            PublicKey publicKey = new KeyPairMock().getPublicKey();
            String address = TrustedEconomicActor.deriveAddressFromPublicKey(publicKey);
            Assert.assertEquals("Address derived from economic actor's public key has incorrect length.", address.length(), Transaction.Field.ADDRESS.tryteLength);
            Assert.assertTrue("Address derived from economic actor's public key is not a tryte sequence: " + address, Trytes.isTrytes(address));
        }
    }
}