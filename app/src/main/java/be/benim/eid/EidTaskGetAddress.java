package be.benim.eid;

import android.content.Context;
import android.support.annotation.StringRes;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.EnumSet;

/**
 * Created by benjamin on 16.07.18.
 */

public class EidTaskGetAddress extends EidTaskRead {

    public EidTaskGetAddress(Context context, CCIDReader reader, JSONObject result) {
        super(context, reader, result, 0XDF01, 0X4033);
        name = "GetAdd";
    }

    @Override
    void process() {
        super.process();

        int len;
        byte[] data = response.toByteArray();
        JSONObject res = new JSONObject();
        for (int i = 0; i< data.length; i+= len) {
            Field field = Field.getField(data[i++]);
            //noinspection StatementWithEmptyBody
            for (len = 0; i < data.length && data[i] == 0XFF; i++, len += 255) {}
            if (i >= data.length || i + (len += data[i]) >= data.length) {
                ((EidView) context).log("Data malformed: len= " + Integer.toString(len));
                break;
            }
            try {
                res.put(context.getString(field.stringId), format(field.encoding,
                        Arrays.copyOfRange(data, ++i, i + len)));
            } catch (JSONException e) {
                ((EidView) context).log("Error putting data into subJSON:\n" + e.getMessage());
                e.printStackTrace();
            }
        }
        try {
            result.put("Address", res);
        } catch (JSONException e) {
            ((EidView) context).log("Error putting subJSon into JSON:\n" +
                    e.getMessage());
        }
    }


    enum Field {
        FILE_STRUC(R.string.stru, 0x00, Encoding.BINARY, 0),
        STREET_NUMB(R.string.stre, 0X01),
        ZIP(R.string.zipc, 0X02, Encoding.ASCII),
        MUNICIPALITY(R.string.muni, 0X03),
        UNKNOWN(R.string.unkn, -0X01, Encoding.ASCII);

        int stringId;
        byte tag;
        Encoding encoding;
        Object standard;

        Field(int stringId, int tag, Encoding encoding, Object standard) {
            this.stringId = stringId;
            this.tag = (byte) tag;
            this.encoding = encoding;
            this.standard = standard;
        }

        Field(@StringRes int stringId, int tag, Object standard) {
            this(stringId, tag, Encoding.UTF8, standard);
        }

        Field(int stringId, byte tag, Encoding encoding) {
            this(stringId, tag, encoding, null);
        }

        Field(@StringRes int stringId, int tag) {
            this(stringId, tag, null);
        }

        static Field getField(byte tag) {
            for (Field field : EnumSet.allOf(Field.class)) {
                if (field.tag == tag)
                    return field;
            }
            return Field.UNKNOWN;
        }
    }

}
