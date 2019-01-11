package org.iota.ict.utils;

import junitparams.FileParameters;
import junitparams.JUnitParamsRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JUnitParamsRunner.class)
public class VersionComparatorTest {

    @Test
    @FileParameters("src/test/resources/utils.VersionComparator_data.csv")
    public void testComparison(String version1, String version2, int expected) {
        VersionComparator underTest = new VersionComparator();

        int actual = underTest.compare(version1, version2);
        int actualInverse = underTest.compare(version2, version1);

        Assert.assertEquals("Failed comparing version numbers.", expected, actual);
        Assert.assertEquals("Failed comparing version numbers.", -expected, actualInverse);
    }
}