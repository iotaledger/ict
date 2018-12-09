package org.iota.ict.utils;

import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;

public class TrytesTest {
    @Test
    public void testNumberEncoding() {
        for (int i = -Trytes.MAX_TRYTE_TRIPLET_ABS; i <= Trytes.MAX_TRYTE_TRIPLET_ABS; i += 30 * Math.random())
            Assert.assertEquals(i, Trytes.toNumber(Trytes.fromNumber(BigInteger.valueOf(i), 3)).intValueExact());
    }

    @Test
    public void testPadding() {
        for (int i = 0; i < 1000; i++) {
            int contentLength = (int) (Math.random() * 20) + 1;
            int padLength = (int) (Math.random() * 10);
            String raw = Trytes.randomSequenceOfLength(contentLength - 1);
            raw += "A"; // make sure last tryte is not 9
            Assert.assertEquals(raw, Trytes.unpadRight(Trytes.padRight(raw, contentLength + padLength)));
        }
    }

    @Test
    public void testAsciiEncoding() {
        for (int i = 0; i < 1000; i++) {
            String tryteTriplet = Trytes.randomSequenceOfLength(3);
            Assert.assertEquals(tryteTriplet.equals("999") ? "" : tryteTriplet, Trytes.fromAscii(Trytes.toAscii(tryteTriplet)));
        }
    }
}