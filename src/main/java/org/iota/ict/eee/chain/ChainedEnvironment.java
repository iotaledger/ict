package org.iota.ict.eee.chain;

import org.iota.ict.eee.Environment;

public class ChainedEnvironment extends Environment {

    public ChainedEnvironment(String chainId) {
        super(chainId + "#chain");
    }
}
