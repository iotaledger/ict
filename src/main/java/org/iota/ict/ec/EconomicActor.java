package org.iota.ict.ec;

import com.iota.curl.IotaCurlHash;
import org.iota.ict.utils.Constants;
import org.iota.ict.utils.Trytes;
import org.iota.ict.utils.crypto.PublicKey;

public abstract class EconomicActor {

    protected final PublicKey publicKey;
    protected final String address;

    public EconomicActor(PublicKey publicKey) {
        if(publicKey == null)
            throw new NullPointerException("'publicKey' is null.");
        this.publicKey = publicKey;
        this.address = deriveAddressFromPublicKey(publicKey);
    }

    static String deriveAddressFromPublicKey(PublicKey publicKey) {
        byte[] keyBytes = publicKey.toBytes();
        String trytes = Trytes.fromBytes(keyBytes, 0, keyBytes.length/2*2 /* /2*2 because must have even length */);
        return IotaCurlHash.iotaCurlHash(trytes, trytes.length(), Constants.CURL_ROUNDS_EC_ADDRESS_DERIVATION);
    }

    public String getAddress() {
        return address;
    }

    protected byte[] messageToSign(String trunk, String branch) {
        return (trunk + branch).getBytes();
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }
}
