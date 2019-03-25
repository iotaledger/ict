package org.iota.ict.eee.call;

import org.iota.ict.eee.Environment;

public class FunctionReturnEnvironment extends Environment {

    public FunctionReturnEnvironment(FunctionEnvironment functionEnvironment) {
        super(functionEnvironment + "#return");
    }
}
