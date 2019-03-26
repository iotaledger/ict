package org.iota.ict.eee.call;

import org.iota.ict.eee.Environment;

public class FunctionEnvironment extends Environment {

    public FunctionEnvironment(String service, String function) {
        super("public/"+service+"/"+function);
    }
}
