package org.iota.ict;

import org.iota.ict.utils.properties.Properties;
import org.junit.Test;

public class IctTest {

    @Test
    public void testStartAndTerminate() {
        Ict ict = new Ict(new Properties().toFinal());
        ict.terminate();
    }
}
