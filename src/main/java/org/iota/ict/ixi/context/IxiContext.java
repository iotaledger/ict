package org.iota.ict.ixi.context;

import org.iota.ict.utils.interfaces.Configurable;

/**
 * This class expands the module-Ict interface with additional features which are not directly related to the Tangle
 * and therefore not part of the actual IXI. It's main purpose is streamlining the working mechanisms of IXI modules,
 * for example by giving them a standard interface to manage configuration.
 * <p>
 * Implementations:
 *
 * @see ConfigurableIxiContext
 * @see SimpleIxiContext
 */
public interface IxiContext extends Configurable {

    String respondToRequest(String request);
}