package org.iota.ict.ixi;


public class VirtualIxiModule extends IxiModule {

    static boolean success = false;

    public VirtualIxiModule(Ixi ixi) {
        super(ixi);
    }

    @Override
    public void run() {
        success = true;
    }
}