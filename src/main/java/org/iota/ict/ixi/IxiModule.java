package org.iota.ict.ixi;

import org.iota.ict.ixi.context.IxiContext;
import org.iota.ict.ixi.context.SimpleIxiContext;
import org.iota.ict.utils.interfaces.Installable;
import org.iota.ict.utils.RestartableThread;

public abstract class IxiModule extends RestartableThread implements Runnable, Installable {

    protected Ixi ixi;

    public IxiModule(Ixi ixi) {
        super(null);
        this.ixi = ixi;
    }

    @Override
    public void install() {

    }

    @Override
    public void uninstall() {

    }

    /***
     * Overwrite this method to set a custom context.
     * @see org.iota.ict.ixi.context.ConfigurableIxiContext if you want to make your IXI configurable.
     * */
    public IxiContext getContext() {
        return SimpleIxiContext.INSTANCE;
    }
}