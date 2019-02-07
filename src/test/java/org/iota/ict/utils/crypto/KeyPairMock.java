package org.iota.ict.utils.crypto;

import java.security.SecureRandom;
import java.util.Arrays;

public class KeyPairMock implements KeyPair {

    private final PublicKey publicKey;
    private final PrivateKey privateKey;

    public KeyPairMock() {
        final byte[] seed = new byte[20];
        new SecureRandom().nextBytes(seed);
        privateKey = new PrivateKeyMock(Arrays.copyOf(seed, seed.length));
        publicKey = new PublicKeyMock(Arrays.copyOf(seed, seed.length));
    }

    @Override
    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    @Override
    public PublicKey getPublicKey() {
        return publicKey;
    }
}
