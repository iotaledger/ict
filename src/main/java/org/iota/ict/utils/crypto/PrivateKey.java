package org.iota.ict.utils.crypto;

public interface PrivateKey {

    /**
     * @param message The message to sign.
     * @return A signature signing the message.
     * */
    byte[] sign(byte[] message);


    /**
     * @param encrypted The encrypted message to decrypt.
     * @return The decrypted message.
     * */
    byte[] decrypt(byte[] encrypted);
}
