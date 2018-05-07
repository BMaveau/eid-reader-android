package be.benim.eid;

import java.io.ByteArrayOutputStream;

/**
 * Created by benjamin on 30.03.18.
 */

public class BulkOutPowerOff extends BulkMessageOut {
    public BulkOutPowerOff(byte slot, byte sequence) {
        super(slot, sequence);
        type= 0x63;
    }

    @Override
    protected ByteArrayOutputStream writeMessage() {
        ByteArrayOutputStream stream= super.writeMessage();
        stream.write(0x00);
        stream.write(0x00);
        stream.write(0x00);
        return stream;
    }
}
