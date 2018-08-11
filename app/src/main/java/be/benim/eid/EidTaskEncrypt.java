package be.benim.eid;

import android.content.Context;
import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

/**
 * Created by benjamin on 17.07.18.
 */

public class EidTaskEncrypt extends EidTask {
    private int indexEncrypts;
    private ArrayList<EidRequest.ToEncrypt> encrypts;

    public EidTaskEncrypt(Context context, CCIDReader reader, JSONObject result,
                          EidRequest request) {
        super(context, reader, result);
        encrypts = request.getToEncrypt();
        Collections.sort(encrypts);
        indexEncrypts = 0;
        name = "Encrypt";
    }

    @Override
    void init() {
        initPhase = true;
        EidRequest.ToEncrypt toEncrypt = encrypts.get(indexEncrypts);
        indexMess = reader.addMessageAndSend(setAlgorithm(toEncrypt.authentication,
                toEncrypt.algorithm));
        if (toEncrypt.authentication)
            reader.addMessageAndSend(verifyPin());
    }

    @Override
    boolean run() {
        BulkMessageIn mess;
        EidRequest.ToEncrypt encryptDone = encrypts.get(indexEncrypts), toEncrypt;
        if (initPhase) {
            if (!checkStatusBytes(mess = reader.getMessage(indexMess++), 0X90, 0X00))
                return quit(mess);
            if (encryptDone.authentication &&
                    !checkStatusBytes(mess = reader.getMessage(indexMess++), 0X90, 0X00))
                return quit(mess);
            initPhase = false;
        } else {
            if (!encryptDone.authentication &&
                    !checkStatusBytes(mess = reader.getMessage(indexMess++), 0X90, 0X00))
                return quit(mess);
            if (checkStatusBytes(mess = reader.getMessage(indexMess), 0X90, 0X00)) {
                String encryptRep = Base64.encodeToString(Arrays.copyOfRange(mess.extra, 0,
                        mess.extra.length - 2), Base64.DEFAULT);
                try {
                    result.put(name + Integer.toString(encryptDone.getIndex()), encryptRep);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else
                return quit(mess);
            if (++indexEncrypts >= encrypts.size()) {
                return ! (done = true);
            }
        }

        toEncrypt = encrypts.get(indexEncrypts);
        if (!toEncrypt.sameMethod(encryptDone)) {
            init();
            return true;
        } else {
            if (toEncrypt.authentication) {
                indexMess = reader.addMessageAndSend(encryptBytes(toEncrypt.toEncrypt));
            } else {
                indexMess = reader.addMessageToQueue(verifyPin());
                reader.addMessageAndSend(encryptBytes(toEncrypt.toEncrypt));
            }
            return true;
        }
    }

    @Override
    void process() {
        if (!error.isEmpty()) {
            try {
                result.put(name, error);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    BulkOutXfg setAlgorithm(boolean authentication, EidRequest.Algorithm algorithm) {
        return new BulkOutXfg(BulkOutXfg.LevelParameter.BEG_AND_END, new byte[] {0X00, 0X22,
                0X41, (byte) 0XB6, 0X05, 0X04, (byte) 0X80, algorithm.value, (byte) 0X84,
                (byte) (authentication ? 0X82 : 0X83)});
    }

    BulkOutXfg encryptBytes(byte[] bytes) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        stream.write(new byte[] {0X00, 0X2A, (byte) 0X9E, (byte) 0X9A, (byte) bytes.length}, 0,
                5);
        stream.write(bytes, 0, bytes.length);
        stream.write(0X00);
        return new BulkOutXfg(BulkOutXfg.LevelParameter.BEG_AND_END, stream.toByteArray());
    }

    BulkSecureVerification verifyPin() {
        return new BulkSecureVerification();
    }
}
