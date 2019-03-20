package org.iota.ict.eee;

public interface EffectDispatcher<T> {

    /**
     * Registers a new EffectListener.
     * @param listener the EffectListener to register
     */
    void addListener(EffectListener<T> listener);

    /**
     * Unregisters a previously registered EffectListener.
     * @param listener the EffectListener to unregister
     */
    void removeListener(EffectListener<T> listener);

    /**
     * Adds an effect to a specific environment queue.
     *
     * @param environment the environment to which the effect should be sent
     * @param effect the effect
     */
    void submitEffect(Environment environment, T effect);
}