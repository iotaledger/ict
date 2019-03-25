package org.iota.ict.ixi;

import org.iota.ict.Ict;
import org.iota.ict.IctTestTemplate;
import org.junit.Assert;
import org.junit.Test;

public class VirtualIxiModuleTest extends IctTestTemplate {

    @Test
    public void test() throws Exception {
        Ict ict = createIct();
        ict.getModuleHolder().loadVirtualModule(VirtualIxiModule.class, "Module");
        ict.getModuleHolder().startAllModules();
        saveSleep(200);
        Assert.assertTrue("run() of virtual IXI module was not invoked", VirtualIxiModule.success);
    }
}
