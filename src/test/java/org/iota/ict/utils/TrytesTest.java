package org.iota.ict.utils;

import org.iota.ict.model.Transaction;
import org.iota.ict.model.TransactionBuilder;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;

public class TrytesTest {
    @Test
    public void testNumberEncoding() {
        for (int i = -Trytes.MAX_TRYTE_TRIPLET_ABS; i <= Trytes.MAX_TRYTE_TRIPLET_ABS; i += 30 * Math.random())
            Assert.assertEquals(i, Trytes.toNumber(Trytes.fromNumber(BigInteger.valueOf(i), 3)).intValue());
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

    @Test
    public void testByteEncoding() {
        String trytes = Trytes.randomSequenceOfLength(3 * (int) (10 * Math.random()));
        byte[] bytes = Trytes.toBytes(trytes);
        Assert.assertEquals(bytes.length, 2 * (int) Math.ceil(trytes.length() / 3.0));
        Assert.assertEquals("decoded bytes did not result in original trytes", trytes, Trytes.fromBytes(bytes));
    }
}