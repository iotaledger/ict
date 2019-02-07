package org.iota.ict.utils.crypto;

public interface PublicKey {

    /**
     * @param signature The signature to validate.
     * @param message The message the signature is supposed to sign.
     * @return <code>true</code> if the signature belongs to this public key and signs the message.
     * */
    boolean verifySignature(byte[] signature, byte[] message);

    /**
     * @param message The message to encrypt.
     * @return The message encrypted with this public key.
     * */
    byte[] encrypt(byte[] message);

    byte[] toBytes();
}