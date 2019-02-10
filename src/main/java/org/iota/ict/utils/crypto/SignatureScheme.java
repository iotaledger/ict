package org.iota.ict.utils.crypto;

import com.iota.curl.IotaCurlHash;
import org.iota.ict.utils.Trytes;

import java.math.BigInteger;

public final class SignatureScheme {

    private static final int HASH_LENGTH = 81;
    private static final int KEY_FRAGMENT_LENGTH = HASH_LENGTH;

    private SignatureScheme() { }

    public static String deriveAddressFromSeed(String seed, int index, int securityLevel) {
        String privateKey = derivePrivateKeyFromSeed(seed, index, securityLevel);
        String publicKey = derivePublicKeyFromPrivateKey(privateKey);
        return deriveAddressFromPublicKey(publicKey);
    }

    public static String derivePrivateKeyFromSeed(String seed, int index, int fragments) {
        String subSeed = hash(seed + Trytes.fromNumber(BigInteger.valueOf(index), 9));
        return derivePrivateKeyFromSubSeed(subSeed, fragments);
    }

    private static String derivePrivateKeyFromSubSeed(String subSeed, int fragments) {
        StringBuilder privateKey = new StringBuilder();
        String lastPrivateKeyFragment = subSeed;
        while (privateKey.length() < KEY_FRAGMENT_LENGTH * 27 * fragments) {
            privateKey.append(lastPrivateKeyFragment = hash(lastPrivateKeyFragment));
        }
        return privateKey.toString();
    }

    public static String deriveAddressFromPrivateKey(String privateKey) {
        return deriveAddressFromPublicKey(derivePublicKeyFromPrivateKey(privateKey));
    }

    public static String deriveAddressFromPublicKey(String publicKey) {
        return hash(publicKey);
    }

    public static String derivePublicKeyFromPrivateKey(String privateKey) {
        StringBuilder publicKey = new StringBuilder();
        for(int privateKeyFragmentIndex = 0; privateKeyFragmentIndex < privateKey.length() / 81; privateKeyFragmentIndex++) {
            int privateKeyFragmentOffset = privateKeyFragmentIndex * KEY_FRAGMENT_LENGTH;
            String privateKeyFragment = privateKey.substring(privateKeyFragmentOffset, privateKeyFragmentOffset + KEY_FRAGMENT_LENGTH);
            String publicKeyFragment = hash(privateKeyFragment, 28);
            publicKey.append(publicKeyFragment);
        }
        return publicKey.toString();
    }

    public static String deriveAddressFromSignature(String signature, String trytes) {
        String publicKey = derivePublicKeyFromSignature(signature, trytes);
        return deriveAddressFromPublicKey(publicKey);
    }

    public static String derivePublicKeyFromSignature(String signatureFragment, String toSign) {
        assert signatureFragment.length() == toSign.length() * 81;
        StringBuilder publicKeyFragments = new StringBuilder();
        for(int i = 0; i < toSign.length(); i++) {
            char tryte = toSign.charAt(i);
            int rounds = Trytes.TRYTES.length() - Trytes.TRYTES.indexOf(tryte);
            String publicKeyFragment = hash(signatureFragment.substring(i * KEY_FRAGMENT_LENGTH, (i+1)*KEY_FRAGMENT_LENGTH), rounds);
            publicKeyFragments.append(publicKeyFragment);
        }
        return publicKeyFragments.toString();
    }

    public static String sign(String privateKey, String trytes, int securityLevel) {
        StringBuilder signature = new StringBuilder();
        for(int bundleHashFragmentIndex = 0; bundleHashFragmentIndex < securityLevel; bundleHashFragmentIndex++) {
            String bundleHashFragment = trytes.substring(bundleHashFragmentIndex * 27, (bundleHashFragmentIndex+1) * 27);
            signature.append(sign(privateKey, bundleHashFragment));
        }
        return signature.toString();
    }

    public static String sign(String privateKey, String trytes) {
        if(privateKey.length() / KEY_FRAGMENT_LENGTH != trytes.length())
            throw new IllegalArgumentException("private key must sign exactly " + (privateKey.length() / KEY_FRAGMENT_LENGTH) + " trytes but " + trytes.length() + " trytes were provided.");
        int privateKeyFragments = privateKey.length() / KEY_FRAGMENT_LENGTH;
        StringBuilder signature = new StringBuilder();
        for(int i = 0; i < trytes.length(); i++) {
            char tryte = trytes.charAt(i);
            int rounds = 1+Trytes.TRYTES.indexOf(tryte);
            int privateKeyFragmentIndex = i%privateKeyFragments;
            String privateKeyFragment = privateKey.substring(privateKeyFragmentIndex * KEY_FRAGMENT_LENGTH, (privateKeyFragmentIndex+1)*KEY_FRAGMENT_LENGTH);
            String signatureFragmentFragment = hash(privateKeyFragment, rounds);
            signature.append(signatureFragmentFragment);
        }
        assert signature.length() == trytes.length()*KEY_FRAGMENT_LENGTH;
        return signature.toString();
    }

    public static String hash(String data, int rounds) {
        assert rounds > 0;
        for(int i = 0; i < rounds; i++)
            data = hash(data);
        return data;
    }

    public static String hash(String data) {
        String hash = IotaCurlHash.iotaCurlHash(data, data.length(), 9);
        assert hash.length() == HASH_LENGTH;
        return hash;
    }
}
