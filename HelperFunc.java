package be.benim.eid;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

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

    static boolean getBit(byte Byte, int pos) {
        return (Byte >> pos) % 2 == 1;
    }
}
