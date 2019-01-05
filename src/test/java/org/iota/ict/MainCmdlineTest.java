package org.iota.ict;

import org.iota.ict.utils.Properties;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Unit test for {@link org.iota.ict.Main.Cmdline}
 */
public class MainCmdlineTest {
    private static final Map<String, Object> DEFAULT_PROPERTIES_MAP = new LinkedHashMap<>();

    private final Main.Cmdline underTest = new Main.Cmdline();

    @BeforeClass
    public static void setupClass() {
        Properties hardcoded = new Properties();
        DEFAULT_PROPERTIES_MAP.put(Properties.Property.anti_spam_abs.name(), hardcoded.antiSpamAbs);
        DEFAULT_PROPERTIES_MAP.put(Properties.Property.anti_spam_rel.name(), hardcoded.antiSpamRel);
        DEFAULT_PROPERTIES_MAP.put(Properties.Property.gui_enabled.name(), hardcoded.guiEnabled);
        DEFAULT_PROPERTIES_MAP.put(Properties.Property.host.name(), hardcoded.host);
        DEFAULT_PROPERTIES_MAP.put(Properties.Property.max_forward_delay.name(), hardcoded.maxForwardDelay);
        DEFAULT_PROPERTIES_MAP.put(Properties.Property.min_forward_delay.name(), hardcoded.minForwardDelay);
        DEFAULT_PROPERTIES_MAP.put(Properties.Property.name.name(), hardcoded.name);
        DEFAULT_PROPERTIES_MAP.put(Properties.Property.neighbors.name(), hardcoded.neighbors.isEmpty() ? "" : hardcoded.neighbors.toString());
        DEFAULT_PROPERTIES_MAP.put(Properties.Property.port.name(), hardcoded.port);
        DEFAULT_PROPERTIES_MAP.put(Properties.Property.round_duration.name(), hardcoded.roundDuration);
        DEFAULT_PROPERTIES_MAP.put(Properties.Property.spam_enabled.name(), hardcoded.spamEnabled);
        DEFAULT_PROPERTIES_MAP.put(Properties.Property.tangle_capacity.name(), hardcoded.tangleCapacity);

        // Check that default properties map contains all property keys
        Properties.Property[] values = Properties.Property.values();
        for (Properties.Property property : values) {
            Assert.assertTrue("Expect property key '" + property.name() + "' not found in default test data.", DEFAULT_PROPERTIES_MAP.containsKey(property.name()));
            Assert.assertNotNull("Expect a non null default test data value.", DEFAULT_PROPERTIES_MAP.get(property.name()));
        }
    }

    @Test
    public void when_parse_without_args_then_defaultValues_used() {
        // when
        Properties ictProperties = underTest.parse(new String[]{}).getIctProperties();

        // then
        assertProperties(DEFAULT_PROPERTIES_MAP, asMap(ictProperties.toPropObject()));
    }

    @Test
    public void when_parse_without_args_and_systemProp_then_systemPropValue_used() {
        // given
        String key = Properties.Property.anti_spam_abs.name();
        String expected = Long.toString(1001);
        String expectedDefault = DEFAULT_PROPERTIES_MAP.get(key).toString();
        Assert.assertNotEquals("Expected value for key '" + key + "' must differ for this test", expected, expectedDefault);

        String resetPropertyOrNull = System.setProperty(key, expected);
        try {
            // when underTest is not init
            Properties ictProperties = underTest.parse(new String[]{}).getIctProperties();

            // then
            String actual = ictProperties.toPropObject().getProperty(key);
            Assert.assertEquals("Expected value for key '" + key + "' differ from default property value", expectedDefault, actual);

            // when underTest is init
            ictProperties = underTest.useSystemProperties()
                    .parse(new String[]{}).getIctProperties();

            // then
            actual = ictProperties.toPropObject().getProperty(key);
            Assert.assertEquals("Expected value for key '" + key + "' differ from system property value", expected, actual);
        } finally {
            if (resetPropertyOrNull != null) {
                System.setProperty(key, resetPropertyOrNull);
            }
        }
    }

    @Test
    public void when_parse_with_configArg_then_configPropValue_used() throws Exception {
        String key = Properties.Property.anti_spam_abs.name();
        String expected = Long.toString(1001);
        String expectedDefault = DEFAULT_PROPERTIES_MAP.get(key).toString();
        Assert.assertNotEquals("Expected value for key '" + key + "' must differ for this test", expected, expectedDefault);

        Path config = Files.createTempFile("ict-", ".cfg");
        Files.write(config, (key + "=" + expected + "\n").getBytes());

        // when
        Properties ictProperties = underTest.parse(new String[]{"--config", config.toString()}).getIctProperties();

        // then
        String actual = ictProperties.toPropObject().getProperty(key);
        Assert.assertEquals("Expected value for key '" + key + "' differ from config-file property value", expected, actual);
    }

    @Test
    public void when_parse_with_configArg_and_systemProp_then_systemPropValue_used() throws Exception {
        // given
        String key = Properties.Property.anti_spam_abs.name();
        String expectedDefault = DEFAULT_PROPERTIES_MAP.get(key).toString();
        String expectedConfig = Long.toString(1001);
        Assert.assertNotEquals("Expected value for key '" + key + "' must differ for this test", expectedConfig, expectedDefault);

        String expectedSystem = Long.toString(1002);
        Assert.assertNotEquals("Expected value for key '" + key + "' must differ for this test", expectedSystem, expectedDefault);
        Assert.assertNotEquals("Expected value for key '" + key + "' must differ for this test", expectedSystem, expectedConfig);

        Path config = Files.createTempFile("ict-", ".cfg");
        Files.write(config, (key + "=" + expectedConfig + "\n").getBytes());

        String resetPropertyOrNull = System.setProperty(key, expectedSystem);
        try {
            // when underTest is not init
            Properties ictProperties = underTest.parse(new String[]{"--config", config.toString()}).getIctProperties();

            // then
            String actual = ictProperties.toPropObject().getProperty(key);
            Assert.assertEquals("Expected value for key '" + key + "' differ from config-file property value", expectedConfig, actual);

            // when underTest is init
            ictProperties = underTest.useSystemProperties()
                    .parse(new String[]{}).getIctProperties();

            // then
            actual = ictProperties.toPropObject().getProperty(key);
            Assert.assertEquals("Expected value for key '" + key + "' differ from system property value", expectedSystem, actual);
        } finally {
            if (resetPropertyOrNull != null) {
                System.setProperty(key, resetPropertyOrNull);
            }
        }
    }

    @Test
    public void when_parse_with_null_args_then_fail() {
        try {
            // when
            underTest.parse(null);
            Assert.fail("NPE expected if args = null");
        } catch (Exception e) {
            // then
            Assert.assertTrue(e instanceof NullPointerException);
            Assert.assertEquals("'args' must not be null.", e.getMessage());
        }
    }

    /*
     *********************
     * Private test helper
     *********************
     */
    private static Map<String, Object> asMap(java.util.Properties properties) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        for (String key : properties.stringPropertyNames()) {
            map.put(key.toString(), properties.getProperty(key));
        }
        return map;
    }

    private static void assertProperties(Map<String, Object> expectedMap, Map<String, Object> actualMap) {
        for (Map.Entry<String, Object> entry : expectedMap.entrySet()) {
            Assert.assertTrue("Expected key '" + entry.getKey() + "' not exist.", actualMap.containsKey(entry.getKey()));
            Assert.assertEquals("Expected value differ for key '" + entry.getKey() + "'", entry.getValue().toString(), actualMap.get(entry.getKey()).toString());
        }
    }
}
