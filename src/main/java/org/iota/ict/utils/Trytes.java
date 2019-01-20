package org.iota.ict.utils;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * This class is a helper tool which allows to perform basic tryte operations, such as conversion between trytes and
 * trits, bytes, numbers and ascii strings. Each tryte String must consist entirely of the uppercase letters A-Z and
 * he digit 9 (see {@link #TRYTES}. Each trit is a number -1, 0 or 1. 3 trits form one tryte (see {@link #TRITS_BY_TRYTE}).
 * <p>
 * When compressed to bytes, 3 trytes (= 9 trits) are stored in 2 bytes. The first byte encodes the first 5 trits, the
 * second byte the other 4 trits.
 */
public final class Trytes {

    public static final String NULL_HASH = "999999999999999999999999999999999999999999999999999999999999999999999999999999999";
    public static final String TRYTES = "9ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    public static final char[] TRYTE_CHARS = TRYTES.toCharArray();
    private static final BigInteger BI3 = BigInteger.valueOf(3), BI27 = BigInteger.valueOf(27);
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
        BigInteger number = BigInteger.ZERO;
        for (int i = trytes.length() - 1; i >= 0; i--) {
            char c = trytes.charAt(i);
            int tryteIndex = c == '9' ? 0 : c-'A'+1;
            byte[] trits = TRITS_BY_TRYTE[tryteIndex];
            number = number.multiply(BI27).add(BigInteger.valueOf(trits[2] * 9 + trits[1] * 3 + trits[0]));
        }
        return number;
    }

    public static long toLong(String trytes) {
        long number = 0;
        for (int i = trytes.length() - 1; i >= 0; i--) {
            char c = trytes.charAt(i);
            int tryteIndex = c == '9' ? 0 : c-'A'+1;
            byte[] trits = TRITS_BY_TRYTE[tryteIndex];
            number = number * 27 + trits[2] * 9 + trits[1] * 3 + trits[0];
        }
        return number;
    }

    public static byte[] toTrits(String trytes) {
        byte[] trits = new byte[trytes.length() * 3];
        for (int i = 0; i < trytes.length(); i++) {
            byte[] tritTriplet = toTrits(trytes.charAt(i));
            trits[3*i+0] = tritTriplet[0];
            trits[3*i+1] = tritTriplet[1];
            trits[3*i+2] = tritTriplet[2];
            //System.arraycopy(tritTriplet, 0, trits, 3 * i, 3);
        }
        return trits;
    }

    public static int sumTrytes(String trytes) {
        int sum = 0;
        for (char c : trytes.toCharArray()) {
            byte[] tritTriplet = toTrits(c);
            sum += tritTriplet[0] + 3 * tritTriplet[1] + 9 * tritTriplet[2];
        }
        return sum;
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
            int intVal = (int)toLong(tryteTriplet) + MAX_TRYTE_TRIPLET_ABS;
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
        byte[] tryteArray = trytes.getBytes();
        for (int i = 0; i < trytes.length() / 3; i++) {
            byte t0 = tryteArray[3*i];
            byte t1 = tryteArray[3*i+1];
            byte t2 = tryteArray[3*i+2];
            int i0 = t0 == '9' ? 0 : t0-'A'+1;
            int i1 = t1 == '9' ? 0 : t1-'A'+1;
            int i2 = t2 == '9' ? 0 : t2-'A'+1;
            bytes[2*i] = (byte) (i0*8 + i2%8);
            bytes[2*i+1] = (byte) (i1*8 + i2/8);
        }
        return bytes;
    }

    public static String fromBytes(byte[] bytes, int bytesOffset, int bytesLength) {
        assert bytesLength % 2 == 0;
        char[] trytes = new char[bytesLength / 2 * 3];
        for (int i = 0; i < trytes.length / 3; i++) {
            int bytesPos = bytesOffset + 2*i;
            int b0 = bytes[bytesPos] < 0 ? bytes[bytesPos] + 256 : bytes[bytesPos];
            int b1 = bytes[bytesPos+1] < 0 ? bytes[bytesPos+1] + 256 : bytes[bytesPos+1];
            trytes[3*i] = TRYTE_CHARS[b0/8];
            trytes[3*i+1] = TRYTE_CHARS[b1/8];
            trytes[3*i+2] = TRYTE_CHARS[b0%8+8*(b1%8)];
        }
        return new String(trytes);
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
