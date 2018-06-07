package be.benim.eid;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.support.annotation.NonNull;

import org.json.JSONException;

import java.nio.charset.StandardCharsets;

/**
 * Created by benjamin on 21.03.18.
 */

class EidReader {
    private CCIDReader reader;
    private EidData data;
    private int index = -1;
    private byte[] currentFile;
    private Context context;
    private Process prevProcess;
    private EidData.Field currentField;

    EidReader(@NonNull Context context) {
        this.context = context;
    }

    void init(UsbManager manager, UsbDevice device) {
        EidView activity = (EidView) context;
        UsbDeviceConnection connection= manager.openDevice(device);
        UsbInterface usbInterface= device.getInterface(0);
        connection.claimInterface(usbInterface, true);
        reader = new CCIDReader(connection, usbInterface);
        new Thread(reader).start();
    }

    void setData(EidData data) {
        this.data = data;
        index = 0;
    }

    void fetchData() {
        reader.addMessageToQueue(selectFile(0X4031));
        reader.addMessageToQueue(readFile(0, 0));
        reader.addMessageAndSend(getResponse(0));
    }

    void processData() {
        EidView activity = (EidView) context;
        for (int i = 0; i < 3; i++) {
            BulkMessageIn messageIn = reader.getMessage(i);
            if (messageIn != null)
                activity.log("Message response: " + messageIn.toHexString());
            else
                activity.log("Null message");
        }
        reader.quitLoop();
//        if (index < 0)
//            return;
//        BulkMessageIn mess;
//        while ((mess = reader.getMessage(index++)) != null) {
//            if (mess.status == 0x00 && mess.type == (byte) 0X80 && mess.last == 0x00) {
//
//            }
//        }
    }

    void nextState() {
        switch (prevProcess) {
            case SELECT:
                read();
                break;
            case READ:
                if (!data.getWantedFields().isEmpty()) {
                    if ((currentField = data.getWantedFields().iterator().next()).fileAddress !=
                            currentFile) {
                        select();
                        break;
                    } else {
                        read();
                        break;
                    }
                }
            case SET:
                encrypt();
                break;
        }
    }

    void handleError() {

    }

    void select() {

    }

    void read() {

    }

    void encrypt() {

    }

    void addResult(EidData.Field field, byte[] bytes) {
        Object value;
        try {
            if (field.encoding == EidData.Encoding.BINARY)
                value = HelperFunc.bytesToInt(bytes);
            else if (field.encoding == EidData.Encoding.ASCII ||
                    field.encoding == EidData.Encoding.UTF8)
                value = new String(bytes, StandardCharsets.UTF_8);
            else
                value = null;
            data.getResult().put(context.getString(field.stringId), value);
            data.getWantedFields().remove(field);
        }
        catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }


    BulkOutXfg selectFile(int file) {
        byte[] fileBytes = HelperFunc.intToByte(file, 2);
        byte[] data = {0x00, (byte) 0XA4, 0x02, 0x0C, 0x02, fileBytes[0], fileBytes[1], 0X00};
        return new BulkOutXfg(BulkOutXfg.LevelParameter.BEG_AND_END, data);
    }

    BulkOutXfg readFile(int offset, int length) {
        byte[] off = HelperFunc.intToByte(offset, 2);
        return new BulkOutXfg(BulkOutXfg.LevelParameter.BEG_AND_END, new byte[] {0x00, (byte) 0xB0,
                off[1], off[0], (byte) length});
    }

    BulkOutXfg getResponse(int length) {
        return new BulkOutXfg(BulkOutXfg.LevelParameter.BEG_AND_END, new byte[] { 0x00,
                (byte) 0xC0, 0x00, 0X00, (byte) length});
    }

    /**
     * The different kinds of process this reader carried out.
     */
    enum Process {
        /**
         * A file is selected
         */
        SELECT,
        /**
         * Data from a file is read.
         */
        READ,
        /**
         * The encryption type is set
         */
        SET,
        /**
         * Data is encrypted
         */
        ENCRYPT
    }
}
