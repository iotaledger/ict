package org.iota.ict.ixi.context;

import org.json.JSONObject;

public class NotConfigurableIxiContext implements IxiContext {

    public static final NotConfigurableIxiContext INSTANCE = new NotConfigurableIxiContext();

    @Override
    public void onUpdateConfiguration(JSONObject newConfiguration) {
        throw new IllegalArgumentException("This IXI module cannot be configured.");
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