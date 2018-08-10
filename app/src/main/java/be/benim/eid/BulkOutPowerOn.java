package be.benim.eid;

import java.io.ByteArrayOutputStream;

/**
 * Created by benjamin on 21.03.18.
 */

public class BulkOutPowerOn extends BulkMessageOut {
    private byte power;

    public BulkOutPowerOn(byte slot, byte sequence) {
        super(slot, sequence);
        power = 0x00;
        type= 0x62;
    }

    public BulkOutPowerOn(byte slot, byte sequence, byte power) {
        super(slot, sequence);
        type= 0x62;
        this.power = power;
    }

    @Override
    protected ByteArrayOutputStream writeMessage() {
        ByteArrayOutputStream stream= super.writeMessage();
        stream.write(power);
        stream.write(0x00);
        stream.write(0x00);
        return stream;
    }
}
