package org.iota.ict.eee.chain;

import org.iota.ict.eee.Environment;

public class ChainIndexEnvironment extends Environment {

    public ChainIndexEnvironment(ChainedEnvironment chainedEnvironment, long position) {
        super(chainedEnvironment + "#" + position);
    }
}
