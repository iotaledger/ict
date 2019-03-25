package org.iota.ict.eee.call;

import org.apache.logging.log4j.LogManager;
import org.iota.ict.eee.dispatch.ThreadedEffectDispatcher;
import org.junit.Test;

import static org.junit.Assert.*;

public class EEEFunctionCallerTest {

    @Test
    public void test() {
        final ThreadedEffectDispatcher dispatcher = new ThreadedEffectDispatcher(LogManager.getLogger("Test"));
        FunctionEnvironment environment = new FunctionEnvironment("Math", "multiply()");
        final EEEFunction function = new EEEFunction(environment);
        EEEFunctionCaller<String> functionCaller = new EEEFunctionCallerImplementation(dispatcher);


        dispatcher.start();

        new Thread(){
            @Override
            public void run() {
                try {
                    EEEFunction.Request request = function.requestQueue.take();
                    String[] args = request.argument.split(",");
                    int arg0 = Integer.parseInt(args[0]);
                    int arg1 = Integer.parseInt(args[1]);
                    request.submitReturn(dispatcher, (arg0*arg1)+"");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();
        dispatcher.addListener(function);

        int arg0 = 3, arg1 = 7;
        String result = functionCaller.call(environment, arg0+","+arg1, 50);
        assertEquals("" + (arg0*arg1), result);

        dispatcher.removeListener(function);
    }
}