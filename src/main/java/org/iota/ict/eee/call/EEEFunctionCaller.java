package org.iota.ict.eee.call;

public interface EEEFunctionCaller<T> {

    T call(FunctionEnvironment environment, T argument, long timeoutMS);
}