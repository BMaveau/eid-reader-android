package be.benim.eid;

/**
 * Created by benjamin on 20.03.18.
 */

abstract class BulkMessage {
    protected byte type;
    protected byte[] length;
    protected byte slot;
    protected byte sequence;

    private BulkMessage() {
        length= new byte[] {0x00, 0x00, 0x00, 0X00};
    }

    BulkMessage(byte slot, byte sequence) {
        this();
        this.slot = slot;
        this.sequence = sequence;
    }

}
