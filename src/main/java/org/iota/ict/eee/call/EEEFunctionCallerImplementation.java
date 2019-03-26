package org.iota.ict.eee.call;

import org.iota.ict.eee.EffectListener;
import org.iota.ict.eee.Environment;
import org.iota.ict.eee.dispatch.EffectDispatcher;

public class EEEFunctionCallerImplementation implements EEEFunctionCaller<String> {

    private final EffectDispatcher dispatcher;

    public EEEFunctionCallerImplementation(EffectDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public String call(FunctionEnvironment environment, String argument, long timeoutMS) {

        String requestID = generateRequestID();
        String request = requestID+";"+argument;
        FunctionReturnEnvironment returnEnvironment = new FunctionReturnEnvironment(environment);
        ReturnListener returnListener = new ReturnListener(returnEnvironment, requestID);

        dispatcher.addListener(returnListener);
        dispatcher.submitEffect(environment, request);
        String response = returnListener.waitForResponse(timeoutMS);
        dispatcher.removeListener(returnListener);
        return response;
    }

    private static String generateRequestID() {
        return "" + System.currentTimeMillis()+Math.random();
    }

    private class ReturnListener implements EffectListener<String> {

        private final FunctionReturnEnvironment environment;
        private final String requestID;
        private String response = null;

        private ReturnListener(FunctionReturnEnvironment environment, String requestID) {
            this.environment = environment;
            this.requestID = requestID;
        }

        @Override
        public void onReceive(String effect) {
            String responseID;
            if(effect.contains(";") && (responseID = effect.split(";")[0]).equals(requestID)) {
                this.response = effect.substring(responseID.length()+1);
                synchronized (this) {
                    this.notify();
                }
            }
        }

        private String waitForResponse(long timeoutMS) {
            if(response == null)
                try {
                    synchronized (this) {
                        this.wait(timeoutMS);
                    }
                } catch (InterruptedException e) { }
            return response;
        }

        @Override
        public Environment getEnvironment() {
            return environment;
        }
    }
}