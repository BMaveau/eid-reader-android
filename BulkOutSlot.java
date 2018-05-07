package be.benim.eid;

import java.io.ByteArrayOutputStream;

/**
 * Created by benjamin on 01.04.18.
 */

class BulkOutSlot extends BulkMessageOut {

    public BulkOutSlot(byte slot, byte sequence) {
        super(slot, sequence);
        type= 0x65;
    }

    @Override
    protected ByteArrayOutputStream writeMessage() {
        ByteArrayOutputStream stream= super.writeMessage();
        for (int i= 0; i< 3; i++)
            stream.write(0x00);
        return stream;
    }
}
