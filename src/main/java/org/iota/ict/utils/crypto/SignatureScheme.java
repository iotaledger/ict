package org.iota.ict.utils.crypto;

/**
 * Abstract description of a signature scheme. For increased modularity and to decouple the signature scheme implementation
 * from the Ict code base as much as possible so that it can be easily replaced.
 * */
public abstract class SignatureScheme {

    public interface PrivateKey<T extends SignatureScheme> {

        int length();

        int fragments();

        String getFragment(int index);

        String deriveAddress();

        SignatureSchemeImplementation.PublicKey derivePublicKey();

        SignatureSchemeImplementation.Signature sign(String toSign);

        String toString();
    }


    public interface PublicKey<T extends SignatureScheme> {

        String getAddress();

        String toString();
    }


    public interface Signature<T extends SignatureScheme> {

        PublicKey<T> derivePublicKey();

        String deriveAddress();

        Signature<T> getFragment(int index);

        int fragments();

        int length();

        String toString();
    }
}
