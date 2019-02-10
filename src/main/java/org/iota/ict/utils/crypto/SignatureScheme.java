package org.iota.ict.utils.crypto;

import com.iota.curl.IotaCurlHash;
import org.iota.ict.utils.Trytes;

import java.math.BigInteger;

public final class SignatureScheme {

    private static final int HASH_LENGTH = 81;
    private static final int KEY_FRAGMENT_LENGTH = HASH_LENGTH;

    private SignatureScheme() { }

    public static String deriveAddressFromSeed(String seed, int index, int securityLevel) {
        PrivateKey privateKey = derivePrivateKeyFromSeed(seed, index, securityLevel);
        PublicKey publicKey = privateKey.derivePublicKey();
        return publicKey.getAddress();
    }

    public static PrivateKey derivePrivateKeyFromSeed(String seed, int index, int fragments) {
        String subSeed = hash(seed + Trytes.fromNumber(BigInteger.valueOf(index), 9));
        return derivePrivateKeyFromSubSeed(subSeed, fragments);
    }

    private static PrivateKey derivePrivateKeyFromSubSeed(String subSeed, int fragments) {
        StringBuilder privateKey = new StringBuilder();
        String lastPrivateKeyFragment = subSeed;
        while (privateKey.length() < KEY_FRAGMENT_LENGTH * 27 * fragments) {
            privateKey.append(lastPrivateKeyFragment = hash(lastPrivateKeyFragment));
        }
        return new PrivateKey(privateKey.toString());
    }

    public static String deriveAddressFromSignature(String signature, String trytes) {
        PublicKey publicKey = derivePublicKeyFromSignature(signature, trytes);
        return publicKey.getAddress();
    }

    public static PublicKey derivePublicKeyFromSignature(String signatureFragment, String toSign) {
        assert signatureFragment.length() == toSign.length() * 81;
        StringBuilder publicKeyFragments = new StringBuilder();
        for(int i = 0; i < toSign.length(); i++) {
            char tryte = toSign.charAt(i);
            int rounds = Trytes.TRYTES.length() - Trytes.TRYTES.indexOf(tryte);
            String publicKeyFragment = hash(signatureFragment.substring(i * KEY_FRAGMENT_LENGTH, (i+1)*KEY_FRAGMENT_LENGTH), rounds);
            publicKeyFragments.append(publicKeyFragment);
        }
        return new PublicKey(publicKeyFragments.toString());
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

    public static class PrivateKey {

        private final String trytes;
        private PublicKey publicKey;

        public PrivateKey(String trytes) {
            if(trytes == null)
                throw new NullPointerException("trytes is null");
            if(trytes.isEmpty() || trytes.length()%KEY_FRAGMENT_LENGTH != 0)
                throw new IllegalArgumentException("invalid length");
            this.trytes = trytes;
        }

        public int length() {
            return trytes.length();
        }

        public int fragments() {
            return length() / KEY_FRAGMENT_LENGTH;
        }

        public String getFragment(int index) {
            return trytes.substring(index * KEY_FRAGMENT_LENGTH, (index+1)*KEY_FRAGMENT_LENGTH);
        }

        public String deriveAddress() {
            return derivePublicKey().getAddress();
        }

        public PublicKey derivePublicKey() {
            if(publicKey == null) {
                StringBuilder publicKeyFragments = new StringBuilder();
                for(int privateKeyFragmentIndex = 0; privateKeyFragmentIndex < trytes.length() / 81; privateKeyFragmentIndex++) {
                    int privateKeyFragmentOffset = privateKeyFragmentIndex * KEY_FRAGMENT_LENGTH;
                    String privateKeyFragment = trytes.substring(privateKeyFragmentOffset, privateKeyFragmentOffset + KEY_FRAGMENT_LENGTH);
                    String publicKeyFragment = SignatureScheme.hash(privateKeyFragment, 28);
                    publicKeyFragments.append(publicKeyFragment);
                }
                this.publicKey = new PublicKey(publicKeyFragments.toString());
            }
            return publicKey;
        }

        public String sign(String toSign) {
            if(length() / KEY_FRAGMENT_LENGTH != toSign.length())
                throw new IllegalArgumentException("private key can only be used to sign exactly " + fragments() + " trytes but " + toSign.length() + " trytes were provided.");
            int privateKeyFragments = length() / KEY_FRAGMENT_LENGTH;
            StringBuilder signature = new StringBuilder();
            for(int i = 0; i < toSign.length(); i++) {
                char tryte = toSign.charAt(i);
                int rounds = 1+Trytes.TRYTES.indexOf(tryte);
                int privateKeyFragmentIndex = i%privateKeyFragments;
                String privateKeyFragment = getFragment(privateKeyFragmentIndex);
                String signatureFragmentFragment = hash(privateKeyFragment, rounds);
                signature.append(signatureFragmentFragment);
            }
            assert signature.length() == toSign.length()*KEY_FRAGMENT_LENGTH;
            return signature.toString();
        }
    }

    public static class PublicKey {
        private final String trytes;
        private final String address;

        public PublicKey(String trytes) {
            this.trytes = trytes;
            this.address = hash(trytes);
        }

        public String getAddress() {
            return address;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof PublicKey && ((PublicKey) obj).address.equals(address);
        }

        @Override
        public int hashCode() {
            return address.hashCode();
        }
    }
}
