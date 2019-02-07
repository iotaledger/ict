package org.iota.ict.utils.crypto;


import java.util.Arrays;

public class PublicKeyMock implements PublicKey {

    private final byte[] byteOverlay;

    public PublicKeyMock(byte[] byteOverlay) {
        this.byteOverlay = Arrays.copyOf(byteOverlay, byteOverlay.length);
    }

    @Override
    public boolean verifySignature(byte[] signature, byte[] message) {
        return Arrays.equals(signature, new PrivateKeyMock(Arrays.copyOf(byteOverlay, byteOverlay.length)).sign(message));
    }

    @Override
    public byte[] encrypt(byte[] message) {
        byte[] encrypted = new byte[message.length];
        for(int i = 0; i < encrypted.length; i++)
            encrypted[i] = (byte)(message[i]+byteOverlay[i%byteOverlay.length]);
        return encrypted;
    }

    @Override
    public byte[] toBytes() {
        return Arrays.copyOf(byteOverlay, byteOverlay.length);
    }
}