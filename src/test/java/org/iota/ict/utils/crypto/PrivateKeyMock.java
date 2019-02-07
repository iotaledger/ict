package org.iota.ict.utils.crypto;

import org.iota.ict.model.Transaction;

import java.util.Arrays;

public class PrivateKeyMock implements PrivateKey {

    private final byte[] byteOverlay;

    public PrivateKeyMock(byte[] byteOverlay) {
        this.byteOverlay = Arrays.copyOf(byteOverlay, byteOverlay.length);
    }

    @Override
    public byte[] sign(byte[] message) {
        byte[] signatureFragment = new byte[Transaction.Field.SIGNATURE_FRAGMENTS.byteLength];
        byte[] signature =  ("signed: " + Arrays.hashCode(message) + " by " + Arrays.hashCode(byteOverlay)).getBytes();
        System.arraycopy(signature, 0, signatureFragment, 0, signature.length);
        signatureFragment[signatureFragment.length-1] = 1; // will be cut away by Trytes if last byte is 0
        return signatureFragment;
    }

    @Override
    public byte[] decrypt(byte[] encrypted) {
        byte[] decrypted = new byte[encrypted.length];
        for(int i = 0; i < decrypted.length; i++)
            decrypted[i] = (byte)(encrypted[i]-byteOverlay[i%byteOverlay.length]);
        return decrypted;
    }
}