package org.iota.ict.eee;

public class ChainIndexEnvironment extends Environment {

    public ChainIndexEnvironment(ChainedEnvironment chainedEnvironment, long position) {
        super(chainedEnvironment + "#" + position);
    }
}
