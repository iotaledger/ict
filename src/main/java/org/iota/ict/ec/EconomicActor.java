package org.iota.ict.ec;

import org.iota.ict.utils.Trytes;
import org.iota.ict.utils.crypto.SignatureSchemeImplementation;

import java.math.BigInteger;

public abstract class EconomicActor {

    protected final String address;

    public EconomicActor(String address) {
        this.address = address;
    }

    public String getAddress() {
        return address;
    }

    protected static String messageToSign(String trunk, String branch) {
        return SignatureSchemeImplementation.hash(trunk + branch);
    }

    protected static String tangleID(String reference1, String reference2) {
        return reference1.compareTo(reference2) < 0 ? reference1 + reference2 : reference2 + reference1;
    }

    protected static String encodeConfidence(double confidence, int padLength) {
        return Trytes.fromNumber(BigInteger.valueOf((long)(confidence * 10000)), padLength);
    }

    protected static double decodeConfidence(String trytes) {
        return Math.max(0, Math.min(1, Trytes.toLong(trytes) / 10000.0));
    }
}