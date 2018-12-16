package org.iota.ict.utils;

import java.math.BigInteger;
import java.util.Arrays;

public class Trytes {

    public static final String NULL_HASH = fromTrits(new byte[81 * 3]);
    static final String TRYTES = "9ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final BigInteger BI3 = BigInteger.valueOf(3);
    static final int MAX_TRYTE_TRIPLET_ABS = 9841; // 9841 = (3^9-1)/2

    public static final byte[][] TRITS_BY_TRYTE = {
            {0, 0, 0}, {1, 0, 0}, {-1, 1, 0}, //9AB
            {0, 1, 0}, {1, 1, 0}, {-1, -1, 1}, //CDE
            {0, -1, 1}, {1, -1, 1}, {-1, 0, 1}, //FGH
            {0, 0, 1}, {1, 0, 1}, {-1, 1, 1}, //IJK
            {0, 1, 1}, {1, 1, 1}, {-1, -1, -1}, //LMN
            {0, -1, -1}, {1, -1, -1}, {-1, 0, -1}, //OPQ
            {0, 0, -1}, {1, 0, -1}, {-1, 1, -1}, //RST
            {0, 1, -1}, {1, 1, -1}, {-1, -1, 0}, //UVW
            {0, -1, 0}, {1, -1, 0}, {-1, 0, 0} //XYZ
    };

    public static BigInteger toNumber(String trytes) {
        byte[] trits = toTrits(trytes);
        BigInteger number = BigInteger.ZERO;
        for (int i = trits.length - 1; i >= 0; i--)
            number = number.multiply(BI3).add(BigInteger.valueOf(trits[i]));
        return number;
    }

    public static byte[] toTrits(String trytes) {
        byte[] trits = new byte[trytes.length() * 3];
        for (int i = 0; i < trytes.length(); i++) {
            byte[] tritTriplet = toTrits(trytes.charAt(i));
            System.arraycopy(tritTriplet, 0, trits, 3 * i, 3);
        }
        return trits;
    }

    private static byte[] toTrits(char tryte) {
        return TRITS_BY_TRYTE[TRYTES.indexOf(tryte)];
    }

    public static String fromTrits(byte[] trits) {
        assert trits.length % 3 == 0;
        byte[] trytes = new byte[trits.length / 3];
        for (int i = 0; i < trytes.length; i++)
            trytes[i] = tryteFromTrits(Arrays.copyOfRange(trits, i * 3, i * 3 + 3), 0);
        return new String(trytes);
    }

    private static byte tryteFromTrits(byte[] trits, int offset) {
        int index = trits[offset + 0] + 3 * trits[offset + 1] + 9 * trits[offset + 2];
        return (byte) TRYTES.charAt((index + 27) % 27);
    }

    public static long toLong(String trytes) {
        return toNumber(trytes).longValue();
    }

    public static String fromAscii(String ascii) {
        assert toNumber(fromNumber(BigInteger.valueOf(9245), 3)).longValue() == 9245;

        if (ascii.length() % 2 != 0)
            ascii += (char) 0;
        char[] trytes = new char[ascii.length() / 2 * 3];
        for (int i = 0; i + 1 < ascii.length(); i += 2) {
            int intVal = ascii.charAt(i) * 127 + ascii.charAt(i + 1) - MAX_TRYTE_TRIPLET_ABS;
            String tryteTriplet = fromNumber(BigInteger.valueOf(intVal), 3);
            trytes[i / 2 * 3 + 0] = tryteTriplet.charAt(0);
            trytes[i / 2 * 3 + 1] = tryteTriplet.charAt(1);
            trytes[i / 2 * 3 + 2] = tryteTriplet.charAt(2);
        }
        return new String(trytes);
    }

    public static String toAscii(String trytes) {
        // unpad 9 triplets
        trytes = unpadRight(trytes);
        trytes = padRight(trytes, trytes.length() + 2 - (trytes.length() + 2) % 3);
        assert trytes.length() % 3 == 0;

        // convert tryte triplets to ascii tuples
        char[] ascii = new char[trytes.length() / 3 * 2];
        for (int i = 0; i < trytes.length() / 3; i++) {
            String tryteTriplet = trytes.substring(3 * i, 3 * i + 3);
            int intVal = toNumber(tryteTriplet).intValue() + MAX_TRYTE_TRIPLET_ABS;
            ascii[2 * i + 0] = (char) (intVal / 127);
            ascii[2 * i + 1] = (char) (intVal % 127);
        }
        String evenSizedAscii = new String(ascii);
        return evenSizedAscii.length() > 0 && evenSizedAscii.charAt(evenSizedAscii.length() - 1) == 0
                ? evenSizedAscii.substring(0, evenSizedAscii.length() - 1)
                : evenSizedAscii;
    }

    public static String padRight(String trytes, int length) {
        char[] padded = new char[length];
        System.arraycopy(trytes.toCharArray(), 0, padded, 0, trytes.length());
        for (int i = trytes.length(); i < length; i++)
            padded[i] = '9';
        return new String(padded);
    }

    public static boolean isTrytes(String string) {
        return string.matches("^[A-Z9]*$");
    }


    static String unpadRight(String padded) {
        int cutPos;
        for (cutPos = padded.length(); cutPos > 0 && padded.charAt(cutPos - 1) == '9'; cutPos--) ;
        return padded.substring(0, cutPos);
    }

    public static String randomSequenceOfLength(int length) {
        char[] sequence = new char[length];
        for (int i = 0; i < length; i++)
            sequence[i] = randomTryte();
        return new String(sequence);
    }

    public static byte[] toBytes(String trytes) {
        assert trytes.length() % 3 == 0;
        byte[] bytes = new byte[trytes.length() / 3 * 2];
        for (int i = 0; i < trytes.length() / 3; i++) {
            byte[] nineTrits = toTrits(trytes.substring(3 * i, 3 * i + 3));
            bytes[2 * i + 0] = tritsToByte(nineTrits, 0, 5);
            bytes[2 * i + 1] = tritsToByte(nineTrits, 5, 4);
        }
        return bytes;
    }

    public static String fromBytes(byte[] bytes) {
        assert bytes.length % 2 == 0;
        byte[] trytes = new byte[bytes.length / 2 * 3];
        for (int i = 0; i < trytes.length / 3; i++) {
            byte[] nineTrits = new byte[9];
            byteToTrits(bytes[2 * i + 0], nineTrits, 0, 5);
            byteToTrits(bytes[2 * i + 1], nineTrits, 5, 4);
            trytes[3 * i + 0] = tryteFromTrits(nineTrits, 0);
            trytes[3 * i + 1] = tryteFromTrits(nineTrits, 3);
            trytes[3 * i + 2] = tryteFromTrits(nineTrits, 6);
        }
        return new String(trytes);
    }

    static byte tritsToByte(byte[] trits, int offset, int length) {
        byte sum = 0;
        int exp = 1;
        for (int i = offset; i < offset + length; i++) {
            sum += exp * trits[i];
            exp *= 3;
        }
        return sum;
    }

    static void byteToTrits(byte b, byte[] target, int offset, int length) {
        byte n = (byte) Math.abs(b);
        for (int i = 0; i < length; i++) {
            int quotient = n / 3;
            int remainder = n % 3;
            if (remainder > 1) {
                target[i + offset] = (byte) (b >= 0 ? -1 : 1);
                n = (byte) (quotient + 1);
            } else {
                target[i + offset] = (byte) (b >= 0 ? remainder : -remainder);
                n = (byte) quotient;
            }
        }
    }

    public static String fromNumber(BigInteger value, int tryteLength) {
        assert value.abs().longValue() <= (Math.pow(3, tryteLength * 3) - 1) / 2;
        final byte[] trits = new byte[tryteLength * 3];

        BigInteger number = value.abs();

        for (int i = 0; i < tryteLength * 3; i++) {
            BigInteger[] divisionResult = number.divideAndRemainder(BI3);
            BigInteger quotient = divisionResult[0];
            BigInteger remainder = divisionResult[1];
            if (remainder.compareTo(BigInteger.ONE) > 0) {
                trits[i] = (byte) (value.signum() >= 0 ? -1 : 1);
                number = quotient.add(BigInteger.ONE);
            } else {
                trits[i] = (byte) ((value.signum() >= 0 ? 1 : -1) * remainder.byteValue());
                number = quotient;
            }
        }

        return fromTrits(trits);
    }

    private static char randomTryte() {
        return Trytes.TRYTES.charAt((int) (Math.random() * 27));
    }
}
