package org.iota.ict.eee.call;

import org.iota.ict.eee.EffectListener;
import org.iota.ict.eee.dispatch.EffectDispatcher;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class EEEFunction implements EffectListener<String> {

    private final FunctionEnvironment environment;
    private final FunctionReturnEnvironment returnEnvironment;
    public final BlockingQueue<Request> requestQueue = new LinkedBlockingQueue<>();

    public EEEFunction(FunctionEnvironment environment) {
        this.environment = environment;
        this.returnEnvironment = new FunctionReturnEnvironment(environment);
    }

    @Override
    public FunctionEnvironment getEnvironment() {
        return environment;
    }

    @Override
    public void onReceive(String effect) {
        requestQueue.add(new Request(effect));
    }

    public class Request {

        private final String requestID;
        public final String argument;

        public Request(String request) {
            this.requestID = request.split(";")[0];
            this.argument = request.contains(";") ? request.substring(requestID.length() + 1) : null;
        }

        public void submitReturn(EffectDispatcher dispatcher, String returnString) {
            dispatcher.submitEffect(returnEnvironment, requestID+";"+returnString);
        }
    }
}
