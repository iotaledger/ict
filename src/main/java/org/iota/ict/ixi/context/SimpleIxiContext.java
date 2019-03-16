package org.iota.ict.ixi.context;

import org.json.JSONObject;

public class SimpleIxiContext implements IxiContext {

    public static final SimpleIxiContext INSTANCE = new SimpleIxiContext();

    @Override
    public String respondToRequest(String request) {
        return "This module has not implemented a response mechanism.";
    }

    @Override
    public void tryToUpdateConfiguration(JSONObject newConfiguration) {
        // do what you are good for, nothing
    }

    @Override
    public JSONObject getConfiguration() {
        return null;
    }

    @Override
    public JSONObject getDefaultConfiguration() {
        return null;
    }
}