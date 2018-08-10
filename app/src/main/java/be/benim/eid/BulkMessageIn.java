package be.benim.eid;

import java.util.Arrays;
import java.util.Locale;

/**
 * Created by benjamin on 20.03.18.
 */

class BulkMessageIn extends BulkMessage {
    byte status;
    byte error;
    byte last;
    byte[] extra;

    BulkMessageIn(byte[] message) {
        super(message[5], message[6]);
        type = message[0];
        length = Arrays.copyOfRange(message, 1, 5);
        status = message[7];
        error = message[8];
        last = message[9];
        if (message.length > 10)
            extra = Arrays.copyOfRange(message, 10, message.length);
        else
            extra = null;
    }

    void addExtra(byte[] extra) {
        if (this.extra == null)
            this.extra = extra;
        else {
            byte[] temp = new byte[extra.length + this.extra.length];
            System.arraycopy(this.extra, 0, temp, 0, this.extra.length);
            System.arraycopy(extra, 0, temp, this.extra.length, extra.length);
            this.extra = temp;
        }
    }

    String getError() {
        return getError(error);
    }

    String getType() {
        return getType(type);
    }

    String toHexString() {
        return "Type: " + HelperFunc.byteToHex(type) + "\tlength: " + Integer.toString(
                HelperFunc.bytesToInt(length)) + "\tStatus: " + HelperFunc.byteToHex(status) +
                "\tError: " + HelperFunc.byteToHex(error) +  "\tlast: " +
                HelperFunc.byteToHex(last) +
                (extra != null ? "\nData: " + HelperFunc.bytesToHex(extra) : "");
    }

    String getString() {
        int commandstatus = (HelperFunc.getBit(status, 7) ? 2 : 0) +
                (HelperFunc.getBit(status, 6) ? 1: 0);
        switch (commandstatus) {
            case 0x01:
                return "The message encountered an error: " + HelperFunc.byteToHex(error) + ": " +
                        getError();
            case 0x02:
                return "A time extension is requested.";
            case 0x00:
                return "No error encountered. Message type: " + getType() + "; last field: " +
                        HelperFunc.byteToHex(last) + (extra==null ? "" :
                        ("; extra part: " + HelperFunc.bytesToHex(extra)));
            case 0x03:
                return "CCID specific error.";
            default:
                return "Should not happen";
        }
    }

    static String getType(byte value) {
        switch (value) {
            case -0x01:
                return "Data block";
            case -0x02:
                return "Slot status";
            case -0x03:
                return "Parameters";
            case -0x04:
                return "Escape";
            case -0x05:
                return "Data rate and clock frequency";
            default:
                return "Unknown message";
        }
    }

    static String getError(byte value) {
        if (value > 0 )
            return String.format(Locale.getDefault(),"message parameter at index %d", value);
        switch (value) {
            case 0:
                return "Command not supported";
            case -1:
                return "Command aborted";
            case -2:
                return "Time out";
            case -3:
                return "Parity error";
            case -4:
                return "Overrun error";
            case -5:
                return "Hardware error";
            case -8:case -9:
                return "Bad ATR";
            case -10:
                return "ICC Protocol supported";
            case -11:
                return "ICC Class not supported";
            case -12:
                return "Procedure byte conflict";
            case -13:
                return "Deactivated protocol";
            case -14:
                return "Busy";
            case -16:
                return "Pin timeout";
            case -17:
                return "Pin cancelled";
            case -32:
                return "Slot busy";
            default:
                return "Future value";
        }
    }

    /**
     * Determines the smart card status from the status byte:
     * @return An integer indicating the status of the smart card
     * 0: Smart card present and active
     * 1: Smart card present but not active
     * 2: No smart card present
     */
    int getSmartCardStatus() {
        if (HelperFunc.getBit(status, 1))
            return 2;
        return HelperFunc.getBit(status, 0) ? 1 : 0;
    }
}
