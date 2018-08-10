package be.benim.eid;

import java.io.ByteArrayOutputStream;

/**
 * Created by benjamin on 15.06.18.
 */

public class BulkMessageSetParam extends BulkMessageOut {
    Protocol protocol = Protocol.T0;
    byte[] param;

    public BulkMessageSetParam(byte slot, Protocol protocol, byte[] param) {
        super(slot, (byte) 0X00);
        type = 0X61;
        length = HelperFunc.intToBytes(param.length);
        this.protocol = protocol;
        this.param = param;
    }

    public BulkMessageSetParam(byte slot, byte[] param) {
        this(slot, Protocol.T0, param);
    }

    @Override
    protected ByteArrayOutputStream writeMessage() {
        ByteArrayOutputStream outputStream = super.writeMessage();
        outputStream.write(protocol.value);
        outputStream.write(0X00);
        outputStream.write(0X00);
        outputStream.write(param, 0, param.length);
        return outputStream;
    }

    enum Protocol {
        T0(0X00), T1(0X01);

        byte value;

        Protocol(int value) {
            this.value = (byte) value;
        }
    }
}
