package org.iota.ict.utils.crypto;

import org.iota.ict.utils.Trytes;

import java.math.BigInteger;

public final class SignatureScheme {

    private static final int HASH_LENGTH = 81;
    private static final int KEY_FRAGMENT_LENGTH = HASH_LENGTH;

    private SignatureScheme() { }

    public static String determineAddressFromSeed(String seed, int index, int securityLevel) {
        String subSeed = hash(seed + Trytes.fromNumber(BigInteger.valueOf(index), 9));
        String privateKey = generatePrivateKey(subSeed, securityLevel);
        String publicKey = derivePublicKeyFromPrivateKey(privateKey);
        return deriveAddressFromPublicKey(publicKey);
    }

    public static String deriveAddressFromPublicKey(String publicKey) {
        return hash(publicKey);
    }

    public static String derivePublicKeyFromPrivateKey(String privateKey) {
        StringBuilder publicKey = new StringBuilder();
        for(int privateKeyFragmentIndex = 0; privateKeyFragmentIndex < privateKey.length() / 81; privateKeyFragmentIndex++) {
            int privateKeyFragmentOffset = privateKeyFragmentIndex * KEY_FRAGMENT_LENGTH;
            String privateKeyFragment = privateKey.substring(privateKeyFragmentOffset, privateKeyFragmentOffset + KEY_FRAGMENT_LENGTH);
            String publicKeyFragment = hash(privateKeyFragment, 81);
            publicKey.append(publicKeyFragment);
        }
        return publicKey.toString();
    }

    public static String generatePrivateKey(String subSeed, int securityLevel) {
        StringBuilder privateKey = new StringBuilder();
        String lastPrivateKeyFragment = subSeed;
        while (privateKey.length() < 81 * 27 * securityLevel) {
            privateKey.append(lastPrivateKeyFragment = hash(lastPrivateKeyFragment));
        }
        return privateKey.toString();
    }

    public static String determineAddressOfSignature(String signatureFragment, String trytes) {
        assert trytes.length() == 27;
        assert signatureFragment.length() == trytes.length() * 81;
        StringBuilder publicKeyFragments = new StringBuilder();
        for(int i = 0; i < trytes.length(); i++) {
            char tryte = trytes.charAt(i);
            int rounds = Trytes.TRYTES.length() - Trytes.TRYTES.indexOf(tryte);
            String publicKeyFragment = hash(signatureFragment.substring(i * KEY_FRAGMENT_LENGTH, (i+1)*KEY_FRAGMENT_LENGTH), rounds);
            publicKeyFragments.append(publicKeyFragment);
        }
        return hash(publicKeyFragments.toString());
    }

    public static String sign(String privateKey, String trytes) {
        assert trytes.length() == 27;
        assert privateKey.length() == trytes.length() * KEY_FRAGMENT_LENGTH;
        StringBuilder signature = new StringBuilder();
        for(int i = 0; i < trytes.length(); i++) {
            char tryte = trytes.charAt(i);
            int rounds = 1+Trytes.TRYTES.indexOf(tryte);
            String privateKeyFragment = privateKey.substring(i * KEY_FRAGMENT_LENGTH, (i+1)*KEY_FRAGMENT_LENGTH);
            signature.append(hash(privateKey, rounds));
        }
        assert signature.length()%(27*KEY_FRAGMENT_LENGTH) == 0;
        return signature.toString();
    }

    public static String hash(String data, int rounds) {
        assert rounds > 0;
        for(int i = 0; i < rounds; i++)
            data = hash(data);
        return data;
    }

    public static String hash(String data) {
        String hash = "";
        assert hash.length() == HASH_LENGTH;
        return hash;
    }
}
