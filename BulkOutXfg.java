package be.benim.eid;

import android.support.annotation.Nullable;

import java.io.ByteArrayOutputStream;

/**
 * Created by benjamin on 01.04.18.
 */

class BulkOutXfg extends BulkMessageOut {
    byte extend;
    LevelParameter parameter;
    private byte[] data;

    /**
     * Constructor of a message to send a data block.
     * @param slot the slot of the smart card
     * @param sequence The sequence number of this command
     * @param extend How much the waiting time must be extenden (default: the maximum value)
     * @param parameter the parameter field. Only required for extended APDU and character
     *                  communication. The default is zero.
     * @param data The data to sent. The default is null
     */
    public BulkOutXfg(byte slot, byte sequence, byte extend, LevelParameter parameter, byte[] data) {
        super(slot, sequence);
        type = 0x6F;
        length = HelperFunc.intToBytes(data == null ? 0 : data.length);
        this.extend = extend;
        this.data = data;
        this.parameter = parameter;
    }

    public BulkOutXfg(byte slot, byte sequence, LevelParameter parameter, byte[] data) {
        this(slot, sequence, (byte) -1, parameter, data);
    }

    public BulkOutXfg(byte slot, byte sequence, @Nullable  byte[] data) {
        this(slot, sequence, (byte) -1, LevelParameter.NA, data);
    }

    public BulkOutXfg(LevelParameter parameter, byte[] data) {
        this((byte)-1, (byte)-1, (byte)-1, parameter, data);
    }

    @Override
    protected ByteArrayOutputStream writeMessage() {
        ByteArrayOutputStream stream = super.writeMessage();
        stream.write(extend);
        stream.write(parameter.getValue(), 0, 2);
        if (data != null && data.length != 0)
            stream.write(data, 0, data.length);
        return stream;
    }

    enum LevelParameter {
        NA(0x00), BEG_AND_END(0x00), BEG_AND_CON(0x01), CON_AND_END(0x02), CON_AND_CON(0x03),
        EMP_AND_CON(0x10);
        int value;

        LevelParameter(int value) {
            this.value = value;
        }

        byte[] getValue() {
            return new byte[] {0x00, (byte) value};
        }
    }
}
