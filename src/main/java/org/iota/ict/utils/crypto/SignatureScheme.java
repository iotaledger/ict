package org.iota.ict.utils.crypto;

import com.iota.curl.IotaCurlHash;
import org.iota.ict.model.Transaction;
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

        public Signature sign(String toSign) {
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
            return new Signature(signature.toString(), toSign);
        }

        @Override
        public String toString() {
            return trytes;
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

        @Override
        public String toString() {
            return trytes;
        }
    }

    public static class Signature {

        // amount of signature trytes that fit into one transaction
        public static final int FRAGMENT_LENGTH = Transaction.Field.SIGNATURE_FRAGMENTS.tryteLength;

        // amount of signature trytes required to sign one tryte
        public static final int FRAGMENT_FRAGMENT_LENGTH = KEY_FRAGMENT_LENGTH;

        // amount of trytes a transaction can sign
        public static final int SIGNED_TRYTES_PER_FRAGMENT = FRAGMENT_LENGTH / FRAGMENT_FRAGMENT_LENGTH;

        private final String trytes;
        private final String signed;
        private PublicKey publicKey;

        public Signature(String trytes, String signed) {
            this.trytes = trytes;
            this.signed = signed;
            assert trytes.length() == signed.length() * KEY_FRAGMENT_LENGTH;
        }

        public PublicKey derivePublicKey() {
            if(publicKey == null) {
                StringBuilder publicKeyFragments = new StringBuilder();
                for(int i = 0; i < signed.length(); i++) {
                    char tryte = signed.charAt(i);
                    int rounds = Trytes.TRYTES.length() - Trytes.TRYTES.indexOf(tryte);
                    String publicKeyFragment = hash(getFragmentFragment(i), rounds);
                    publicKeyFragments.append(publicKeyFragment);
                }
                publicKey = new PublicKey(publicKeyFragments.toString());
            }
            return publicKey;
        }

        private String getFragmentFragment(int index) {
            return  trytes.substring(index * KEY_FRAGMENT_LENGTH, (index+1)*KEY_FRAGMENT_LENGTH);
        }

        public String deriveAddress() {
            return derivePublicKey().address;
        }

        public Signature getFragment(int index) {
            if(index < 0 || index >= fragments())
                throw new IndexOutOfBoundsException(index + " not in interval [0,"+fragments()+"]");
            String tryteFragment = trytes.substring(index * FRAGMENT_LENGTH, (index+1)+FRAGMENT_LENGTH);
            String signedFragment = signed.substring(index * SIGNED_TRYTES_PER_FRAGMENT, (index+1) * SIGNED_TRYTES_PER_FRAGMENT);
            return new Signature(tryteFragment, signedFragment);
        }

        public int fragments() {
            return length() / FRAGMENT_LENGTH;
        }

        @Override
        public String toString() {
            return trytes;
        }

        public int length() {
            return trytes.length();
        }
    }
}
