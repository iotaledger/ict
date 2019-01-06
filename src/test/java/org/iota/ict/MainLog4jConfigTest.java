package org.iota.ict;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class MainLog4jConfigTest {
    private static final String EXPECTED_TO_STRING_TEMPLATE = "Log4jConfig{\n" +
            " logLevel=${LEVEL},\n" +
            " isEnabled(logLevel-setting)=false,\n" +
            " logDir=logs/,\n" +
            " logFilename=ict.log,\n" +
            " logFileEnabled=false\n" +
            "}";
    private static final List<String> KNOWING_LOG4J_CONTEXT_VARIABLES = Arrays.asList("logFileEnabled", "logDir", "logFilename");

    private final Main.Log4JConfig underTest = Main.Log4JConfig.getDefault();

    @Test
    public void default_toString_test() {
        Assert.assertEquals(EXPECTED_TO_STRING_TEMPLATE.replace("${LEVEL}", "INFO"), underTest.toString());
    }

    @Test
    public void when_enableDebug_then_debug_isSet() {
        underTest.enableDebug();
        Assert.assertEquals(EXPECTED_TO_STRING_TEMPLATE.replace("${LEVEL}", "DEBUG"), underTest.toString());
    }

    @Test
    public void when_enableDebug_then_trace_isSet() {
        underTest.enableTrace();
        Assert.assertEquals(EXPECTED_TO_STRING_TEMPLATE.replace("${LEVEL}", "TRACE"), underTest.toString());
    }

    @Test
    public void when_enableLogToFile_then_dirAndFile_isSet() {
        underTest.enableLogToFile("test-log-dir", "test-log-filename");
        Assert.assertEquals(EXPECTED_TO_STRING_TEMPLATE
                .replace("${LEVEL}", "INFO")
                .replace("logs/", "test-log-dir")
                .replace("ict.log", "test-log-filename")
                .replace("logFileEnabled=false", "logFileEnabled=true"), underTest.toString());
    }

    @Test
    public void verify_that_all_log4jsXml_keyAttributes_are_knownCtxVariables() throws URISyntaxException, IOException {
        List<String> log4jXmlLines = Files.readAllLines(
                Paths.get(this.getClass().getResource("/log4j2.xml").toURI()), Charset.defaultCharset());

        for (String line : log4jXmlLines) {
            String keyAttributeValue = extractKeyAttributeValueOrNull(line);
            if (keyAttributeValue != null) {
                Assert.assertTrue("Found unknown ctx:variable '" + keyAttributeValue + "' as <xml key=${ctx:variable}> in 'classpath:log4j2.xml' file", KNOWING_LOG4J_CONTEXT_VARIABLES.contains(keyAttributeValue));
            }
        }
    }

    @Test
    public void verify_that_all_log4jsXml_ctxVariables_are_knownCtxVariables() throws URISyntaxException, IOException {
        List<String> log4jXmlLines = Files.readAllLines(
                Paths.get(this.getClass().getResource("/log4j2.xml").toURI()), Charset.defaultCharset());

        for (String line : log4jXmlLines) {
            List<String> ctxVariables = extractCtxVariables(line, "${ctx:", "}");
            for (String ctxVariable : ctxVariables) {
                Assert.assertTrue("Found unknown ctx:variable '" + ctxVariable + "' as <xml key=${ctx:variable}> in 'classpath:log4j2.xml' file", KNOWING_LOG4J_CONTEXT_VARIABLES.contains(ctxVariable));
            }
        }
    }

    /*
     *********************
     * Private test helper
     *********************
     */

    /**
     * Try extract first 'key="VALUE"' in line.
     */
    private String extractKeyAttributeValueOrNull(String line) {
        String keyAttributePrefix = "key=\"";
        String keyAttributeSuffix = "\"";
        String keyAttributeValue = null;
        int startIndexOfKeyAttribute = line.indexOf(keyAttributePrefix);
        if (startIndexOfKeyAttribute != -1) {
            int startIndex = startIndexOfKeyAttribute + keyAttributePrefix.length();
            int endIndex = line.indexOf(keyAttributeSuffix, startIndex);
            keyAttributeValue = line.substring(startIndex, endIndex);
        }
        return keyAttributeValue;
    }

    /**
     * Try extract all '${ctx:VARIABLE}' in line.
     */
    private List<String> extractCtxVariables(String line, String prefix, String suffix) {
        LinkedList<String> ctxVariables = new LinkedList<>();
        String ctxVariable = null;
        int startIndexOfCtxVariable = 0;
        while (true) {
            startIndexOfCtxVariable = line.indexOf(prefix, startIndexOfCtxVariable);
            if (startIndexOfCtxVariable == -1) {
                break;
            }
            int startIndex = startIndexOfCtxVariable + prefix.length();
            int endIndex = line.indexOf(suffix, startIndex);
            ctxVariable = line.substring(startIndex, endIndex);
            ctxVariables.add(ctxVariable);

            startIndexOfCtxVariable = endIndex;
        }
        return ctxVariables;
    }
}
