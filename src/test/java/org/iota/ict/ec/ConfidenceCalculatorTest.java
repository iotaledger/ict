package org.iota.ict.ec;

import org.junit.Assert;
import org.junit.Test;

import java.util.*;

public class ConfidenceCalculatorTest {

    @Test
    public void test() {

        List<String> tangles = Arrays.asList("a", "b", "c", "d");

        Set<ConfidenceCalculator.Conflict> conflicts = new HashSet<>();
        conflicts.add(new ConfidenceCalculator.Conflict("a", "b"));
        conflicts.add(new ConfidenceCalculator.Conflict("c", "d"));

        ConfidenceCalculator confidenceCalculator = new ConfidenceCalculator(tangles, conflicts);

        Assert.assertEquals("Unexpected confidence", 0.5, confidenceCalculator.confidenceOf("a"), 1E-3);
        Assert.assertEquals("Unexpected confidence", 0.5, confidenceCalculator.confidenceOf("b"), 1E-3);
        Assert.assertEquals("Unexpected confidence", 0.5, confidenceCalculator.confidenceOf("c"), 1E-3);
        Assert.assertEquals("Unexpected confidence", 0.5, confidenceCalculator.confidenceOf("d"), 1E-3);
    }

}