package be.benim.eid;

import android.support.annotation.CallSuper;

import java.io.ByteArrayOutputStream;

/**
 * Created by benjamin on 17.04.18.
 */

public class BulkMessageOut extends BulkMessage {

    public BulkMessageOut(byte slot, byte sequence) {
        super(slot, sequence);
    }

    @CallSuper
    protected ByteArrayOutputStream writeMessage() {
        ByteArrayOutputStream stream= new ByteArrayOutputStream();
        stream.write(type);
        stream.write(length, 0, 4);
        stream.write(slot);
        stream.write(sequence);
        return stream;
    }

    byte[] getMessage() {
        return writeMessage().toByteArray();
    }

}
