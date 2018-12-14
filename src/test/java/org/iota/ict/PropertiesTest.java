package org.iota.ict;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.net.InetSocketAddress;

public class PropertiesTest {

    @Test
    public void testCloneProperties() {
        Properties original = createCustomProperties();
        Properties copy = new Properties(original.toPropObject());
        Assert.assertEquals("failed cloning properties", original.toPropObject(), copy.toPropObject());
    }

    @Test
    public void testStoreProperties() {
        Properties original = createCustomProperties();

        String path = "properties_test_file" + Math.random() + ".prop";
        original.store(path);
        Properties copy = Properties.fromFile(path);
        new File(path).delete();

        Assert.assertEquals("failed storing properties", original.toPropObject(), copy.toPropObject());
    }

    private Properties createCustomProperties() {
        Properties custom = new Properties();
        custom.port = (int)(Math.random() * 10000)+1;
        custom.host = "example.org";
        custom.minForwardDelay = 50;
        custom.maxForwardDelay = 4000;
        custom.logRoundDuration = 15000;
        custom.neighbors.add(new InetSocketAddress("http://example.org", 443));
        custom.neighbors.add(new InetSocketAddress("https://example.com", 1337));
        return custom;
    }
}