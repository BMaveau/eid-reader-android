package be.benim.eid;

import static be.benim.eid.HelperFunc.getBit;

/**
 * Created by benjamin on 13.06.18.
 */

public class SmartCard {
    private byte fi = 0b0001;
    private byte di = 0b0001;
    private int N = 0;
    private int wi = 0x0a;

    /**
     * Generates the smart card parameters based on the answer to reset. Currently only supported
     * for T=0.
     * @param atr the answer to reset. See iso pdf for specifications.
     * TODO: add support for other transmission protocols.
     */
    public SmartCard(byte[] atr) {
        int index = 0;
        int K = atr[0] & 0X0F;
        if (getBit(atr[0], 4)) {
//            TA1
            index++;
            fi = (byte) ((atr[index] >>> 4) & 0X0F);
            di = (byte) (atr[index] & 0X0F);
        }
        if (getBit(atr[0], 5))
//            TB1
            index++;
        if (getBit(atr[0], 6)) {
//            TC1
            index++;
            N = atr[index];
        }
        if (getBit(atr[0], 7)) {
//            TD1
            index++;
            if (getBit(atr[index], 6)) {
//                TC2
                index += (getBit(atr[index], 4) ? 1 : 0) + (getBit(atr[index], 5) ? 0 : 1);
                wi = atr[index];
            }
        }
    }


    /**
     * Generate the parameters for T0 communication
     * @return an array of bytes with the required parameters
     * TODO: set fi and di from ATR instead of default value.
     */
    byte[] generateT0() {
        byte[] dataStructure = new byte[5];
//        dataStructure[0] = (byte) ((fi << 4) + di);
        dataStructure[0] = 0X11;
        dataStructure[1] = 0x00;
        dataStructure[2] = (byte) N;
        dataStructure[3] = (byte) wi;
        dataStructure[4] = 0X00;
        return dataStructure;
    }

    @Override
    public String toString() {
        return "Smartcard: fi= " + HelperFunc.byteToHex(fi) + " di= " + HelperFunc.byteToHex(di) +
                " N= " + Integer.toString(N) + " wi= " + Integer.toString(wi);
    }
}
