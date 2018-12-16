package org.iota.ict.utils;

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

    @Test
    public void testByteTritEncoding() {
        int tritLength = (int) (Math.random() * 5);
        int maxIntValue = (int) (Math.pow(3, tritLength) - 1) / 2;
        byte[] rawTrits = Trytes.toTrits(Trytes.fromNumber(BigInteger.valueOf(maxIntValue), tritLength));

        byte encodedTrits = Trytes.tritsToByte(rawTrits, 0, rawTrits.length);
        byte[] decodedTrits = new byte[rawTrits.length];
        Trytes.byteToTrits(encodedTrits, decodedTrits, 0, rawTrits.length);
        Assert.assertEquals("trit-byte encoding does not work", tritsAsString(rawTrits), tritsAsString(decodedTrits));
    }

    private String tritsAsString(byte[] trits) {
        char[] readable = new char[trits.length];
        for (int i = 0; i < trits.length; i++)
            readable[i] = trits[i] == 1 ? '1' : trits[i] == -1 ? '-' : '0';
        return new String(readable);
    }
}