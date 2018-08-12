package be.benim.eid;

import android.content.Context;
import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Created by benjamin on 07.07.18.
 */

public class EidTaskRead extends EidTask {
    private static int currentDir = -1;
    private int dirReference;
    private int fileReference;
    private int offset;
    private int len;
    private boolean fullInit;

    EidTaskRead(Context context, CCIDReader reader, JSONObject result, int dirReference,
                int fileReference, String name) {
        super(context, reader, result);
        this.dirReference = dirReference;
        this.fileReference = fileReference;
        this.name = name;
        fullInit = false;
        offset = 0;
        len = 0;
    }

    protected EidTaskRead(Context context, CCIDReader reader, JSONObject result, int dirReference,
                       int fileReference) {
        super(context, reader, result);
        this.dirReference = dirReference;
        this.fileReference = fileReference;
        fullInit = false;
        offset = 0;
        len = 0;
    }

    @Override
    void init() {
        initPhase = true;
        if (fullInit || dirReference != currentDir) {
            fullInit = true;
            indexMess = reader.addMessageToQueue(selectFile(0X3F00));
            reader.addMessageToQueue(selectFile(dirReference));
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
                currentDir = dirReference;
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
                offset += (len == 0 ? mess.extra.length - 2 : len);
                if (mess.extra.length - 2 != (len == 0 ? 256 : len))
                    return ! (done = true);
            } else if (checkStatusBytes(mess, 0X6C))
                len = mess.extra[mess.extra.length - 1];
            else if (checkStatusBytes(mess, 0X6B, 0X00))
                return ! (done = true);
            else
                return quit(mess);
        }

        indexMess = reader.addMessageAndSend(readFile());
        return true;
    }

    @Override
    void process() {
        byte[] data = response.toByteArray();
        try {
            if (error.isEmpty()) {
                int i;
                for (i = data.length-1; i >= 0 && data[i] == 0X00; i--) {}
                result.put(name, Base64.encodeToString(
                        Arrays.copyOfRange(data, 0, i+1), Base64.DEFAULT));
            }
            else
                result.put(name, error);
        } catch (JSONException e) {
            e.printStackTrace();
            ((EidView) context).log("Error putting data into JSON:\n" + e.getMessage());
        }
    }

    Object format(Encoding encoding, byte[] value) {
        if (encoding == Encoding.BINARY)
            return HelperFunc.bytesToHex(value);
        else if (encoding == Encoding.ASCII || encoding == Encoding.UTF8)
            return new String(value, StandardCharsets.UTF_8);
        else
            return null;
    }

    private BulkOutXfg selectFile(int file) {
        byte[] fileBytes = HelperFunc.intToByte(file, 2);
        byte[] data = {0x00, (byte) 0XA4, 0x02, 0x0C, 0x02, fileBytes[1], fileBytes[0]};
        return new BulkOutXfg(BulkOutXfg.LevelParameter.BEG_AND_END, data);
    }

    private BulkOutXfg readFile() {
        byte[] off = HelperFunc.intToByte(offset, 2);
        return new BulkOutXfg(BulkOutXfg.LevelParameter.BEG_AND_END, new byte[] {0x00,
                (byte) 0xB0, off[1], off[0], (byte) len});
    }

    enum Encoding {
        BINARY, ASCII, UTF8
    }
}
