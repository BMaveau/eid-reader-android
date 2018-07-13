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
        if (request.getID && !result.has("GetId"))
            return new EidTaskGetID(context, reader, result);
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

    protected BulkOutXfg getResponse(int length) {
        return new BulkOutXfg(BulkOutXfg.LevelParameter.BEG_AND_END, new byte[] { 0x00,
                (byte) 0xC0, 0x00, 0X00, (byte) length});
    }
}
