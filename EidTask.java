package be.benim.eid;

import android.content.Context;
import android.support.annotation.Nullable;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;

/**
 * Created by benjamin on 07.07.18.
 */

public abstract class EidTask {
    boolean initPhase;
    boolean done;
    int indexMess;
    JSONObject result;
    ByteArrayOutputStream response;
    CCIDReader reader;
    String error;
    String name;
    Context context;


    public EidTask(Context context, CCIDReader reader, JSONObject result) {
        this.context = context;
        this.reader = reader;
        initPhase = true;
        done = false;
        this.result = result;
        response = new ByteArrayOutputStream();
        error = "";
    }

    public JSONObject getResult() {
        if (!done)
            return null;
        if (!result.has(name))
            process();
        return result;
    }

    @Nullable static EidTask next(Context context, EidRequest request, CCIDReader reader,
                                  JSONObject result) {
        if ((request.getID || request.checkID || request.checkPic) && !result.has("GetId"))
            return new EidTaskGetID(context, reader, result);
        else if ((request.checkID || request.checkAddress || request.checkPic)
                && ! result.has("SignId"))
            return new EidTaskRead(context, reader, result, 0XDF01, 0X4032,
                    "SignId");
        else if ((request.getAddress || request.checkAddress) && ! result.has("GetAdd"))
            return new EidTaskGetAddress(context, reader, result);
        else if (request.checkAddress && !result.has("SignAdd"))
            return new EidTaskRead(context, reader, result, 0XDF01, 0X4034,
                    "SignAdd");
        else if (request.getPic && ! result.has("GetPic"))
            return new EidTaskRead(context, reader, result, 0XDF01, 0X4035,
                    "GetPic");
        else if ((request.checkID || request.checkAddress || request.checkPic) &&
                !result.has("Cert#8(RN)"))
            return new EidTaskCheckSignature(context, reader, result, request);
        else
            return null;
    }

    /**
     * Initialised this task
     */
    abstract void init();
    /**
     * Contains the part which should run indefinitely. First part should be reading the responses.
     * In the second par new messages are sent to the smartcard.
     * @return False if the task is done.
     */
    abstract boolean run();
    abstract void process();

    boolean checkMessage(@Nullable BulkMessageIn mess) {
        if (mess == null)
            return false;

        boolean status = true;
        int len = 0;
        if (mess.extra != null && (len = mess.extra.length) >=2)
            status = (mess.extra[len - 2] == (byte) 0X90 && mess.extra[len - 1] == 0X00);
        return mess.type == (byte) 0X80 && mess.error == 0X00 && status;
    }

    boolean checkStatusBytes(BulkMessageIn mess, int sw1, int sw2) {
        int len;
        return mess != null && mess.type == (byte) 0X80 && mess.error == 0X00 &&
                mess.extra != null && (len = mess.extra.length) > 1 &&
                mess.extra[len-2] == (byte) sw1 && mess.extra[len-1] == (byte) sw2;
    }

    boolean checkStatusBytes(BulkMessageIn mess, int sw1) {
        int len;
        return mess != null && mess.type == (byte) 0X80 && mess.error == 0X00 &&
                mess.extra != null && (len = mess.extra.length) > 1 &&
                mess.extra[len-2] == (byte) sw1;
    }

    protected boolean quit(BulkMessageIn mess) {
        done = true;
        error = "Selecting/reading the file failed" +
                (mess == null ? "." : ": " + mess.toHexString());
        return false;
    }

    protected BulkOutXfg getResponse(int length) {
        return new BulkOutXfg(BulkOutXfg.LevelParameter.BEG_AND_END, new byte[] { 0x00,
                (byte) 0xC0, 0x00, 0X00, (byte) length});
    }
}
