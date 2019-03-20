package org.iota.ict.eee;

public class ChainedEnvironment extends Environment {

    public ChainedEnvironment(String chainId) {
        super(chainId + "_chain");
    }
}
