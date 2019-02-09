package org.iota.ict.ec;

public abstract class EconomicActor {

    protected final String address;

    public EconomicActor(String address) {
        this.address = address;
    }

    public String getAddress() {
        return address;
    }

    protected String messageToSign(String trunk, String branch) {
        return trunk + branch;
    }
}