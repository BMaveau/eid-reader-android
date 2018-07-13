package be.benim.eid;

import android.content.Context;
import android.support.annotation.Nullable;

import org.json.JSONObject;

/**
 * Created by benjamin on 07.07.18.
 */

public abstract class EidTaskRead extends EidTask {
    private int fileReference;
    private int offset;
    private int len;
    private boolean fullInit;

    public EidTaskRead(Context context, CCIDReader reader, JSONObject result, int fileReference) {
        super(context, reader, result);
        this.fileReference = fileReference;
        fullInit = false;
        offset = 0;
        len = 0;
    }

    @Override
    void init() {
        initPhase = true;
        if (fullInit) {
            indexMess = reader.addMessageToQueue(selectFile(0X3F00));
            reader.addMessageToQueue(selectFile(0XDF01));
            reader.addMessageAndSend(selectFile(fileReference));
        } else
            indexMess = reader.addMessageAndSend(selectFile(fileReference));
    }

    @Override
    boolean run() {
        BulkMessageIn mess;
        int lenExtra;
        if (initPhase) {
            if (fullInit) {
                if (!checkMessage(mess = reader.getMessage(indexMess++)) ||
                        !checkMessage(mess = reader.getMessage(indexMess++)) ||
                        !checkMessage(mess = reader.getMessage(indexMess++))) {
                    return quit(mess);
                }

            } else {
                if (!checkMessage(mess = reader.getMessage(indexMess++))) {
                    if (mess != null && (lenExtra = mess.extra.length) == 2 &&
                            mess.extra[lenExtra - 2] == 0X6A &&
                            mess.extra[lenExtra - 1] == (byte) 0X82) {
                        fullInit = true;
                        init();
                        return true;
                    } else
                        return quit(mess);
                }
            }
            initPhase = false;
        } else {
            if (checkStatusBytes(mess = reader.getMessage(indexMess++), 0X90, 0X00)) {
                response.write(mess.extra, 0, mess.extra.length - 2);
                offset += (len == 0 ? 255 : len);
                if (len != 0 || mess.extra.length != 255)
                    return false;
            } else if (checkStatusBytes(mess, 0X6C))
                len = mess.extra[mess.extra.length - 1];
            else
                return quit(mess);
        }

        indexMess = reader.addMessageAndSend(readFile());
        return true;
    }

    private boolean quit(BulkMessageIn mess) {
        done = true;
        error = "Selecting/reading the file failed" +
                (mess == null ? "." : ": " + mess.toHexString());
        return false;
    }

    private boolean checkMessage(@Nullable BulkMessageIn mess) {
        if (mess == null)
            return false;

        boolean status = true;
        int len = 0;
        if (mess.extra != null && (len = mess.extra.length) >=2)
            status = (mess.extra[len - 2] == (byte) 0X90 && mess.extra[len - 1] == 0X00);
        return mess.type == (byte) 0X80 && mess.error == 0X00 && status;
    }

    private boolean checkStatusBytes(BulkMessageIn mess, int sw1, int sw2) {
        int len;
        return mess != null && mess.type == (byte) 0X80 && mess.error == 0X00 &&
                mess.extra != null && (len = mess.extra.length) > 1 &&
                mess.extra[len-2] == (byte) sw1 && mess.extra[len-1] == (byte) sw2;
    }

    private boolean checkStatusBytes(BulkMessageIn mess, int sw1) {
        int len;
        return mess != null && mess.type == (byte) 0X80 && mess.error == 0X00 &&
                mess.extra != null && (len = mess.extra.length) > 1 &&
                mess.extra[len-2] == (byte) sw1;
    }


    private BulkOutXfg selectFile(int file) {
        byte[] fileBytes = HelperFunc.intToByte(file, 2);
        byte[] data = {0x00, (byte) 0XA4, 0x02, 0x0C, 0x02, fileBytes[1], fileBytes[0]};
        return new BulkOutXfg(BulkOutXfg.LevelParameter.BEG_AND_END, data);
    }

    private BulkOutXfg readFile() {
        byte[] off = HelperFunc.intToByte(offset, 2);
        return new BulkOutXfg(BulkOutXfg.LevelParameter.BEG_AND_END, new byte[] {0x00, (byte) 0xB0,
                off[1], off[0], (byte) len});
    }
}
