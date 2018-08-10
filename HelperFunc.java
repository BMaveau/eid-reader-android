package be.benim.eid;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Created by benjamin on 18.04.18.
 */

public class HelperFunc {
    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    static String byteToHex(byte byt) {
        char[] hexChars = new char[2];
        int v = byt & 0xFF;
        hexChars[0] = hexArray[v >>> 4];
        hexChars[1] = hexArray[v & 0x0F];
        return new String(hexChars);
    }

    static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 3];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 3] = hexArray[v >>> 4];
            hexChars[j * 3 + 1] = hexArray[v & 0x0F];
            hexChars[j * 3 + 2] = ' ';
        }
        return new String(hexChars);
    }

    static String intToHex(int number) {
        ByteBuffer buffer= ByteBuffer.allocate(4);
        return bytesToHex(buffer.putInt(number).array());
    }

    static int bytesToInt(byte[] bytes) {
        ByteBuffer byteBuffer= ByteBuffer.wrap(bytes);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        return byteBuffer.getInt();
    }

    static byte[] intToBytes(int number) {
        return intToByte(number, 4);
    }

    static byte[] intToByte(int number, int len) {
        byte[] ret = new byte[4];
        ((ByteBuffer) ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
                .putInt(number).position(0)).get(ret);
        return Arrays.copyOfRange(ret, 0, len);
    }

    static byte[] hexStringToByteArray(String s) {
        s = s.replaceAll("\\s+","");
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    static boolean getBit(byte Byte, int pos) {
        return (Byte >> pos) % 2 == 1;
    }
}
