package org.iota.ict.utils.interfaces;

import org.json.JSONObject;

public interface Configurable {

    /**
     * Is called when the configuration is externally updated. The implementation can either accept the changes and update
     * the internal configuration or throw an Exception with a meaningful message to inform the caller about why the changes
     * cannot be applied (invalid format, invalid length, illegal characters, illegal combination of flags, etc.).
     */
    void tryToUpdateConfiguration(JSONObject newConfiguration);

    /**
     * @return The current internal configuration of the IXI module.
     */
    JSONObject getConfiguration();

    /**
     * @return The default configuration which will be used when the user resets the configuration. This one should equal
     * the internal configuration right after installation.
     */
    JSONObject getDefaultConfiguration();
}
