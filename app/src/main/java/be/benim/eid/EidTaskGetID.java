package be.benim.eid;

import android.content.Context;
import android.support.annotation.StringRes;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.EnumSet;

/**
 * Created by benjamin on 07.07.18.
 */

public class EidTaskGetID extends EidTaskRead {

    public EidTaskGetID(Context context, CCIDReader reader, JSONObject result) {
        super(context, reader, result, 0XDF01, 0X4031);
        name = "GetId";
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
            result.put("ID", res);
        } catch (JSONException e) {
            ((EidView) context).log("Error putting subJSon into JSON:\n" +
                    e.getMessage());
        }
    }


    enum Field {
        FILE_STRUC(R.string.stru, 0x00, Encoding.BINARY, 0),
        CART_NUM(R.string.canu, 0X01, Encoding.ASCII),
        CHIP_NIM(R.string.chnu, 0X02, Encoding.BINARY),
        VALID_FROM(R.string.cvbe, 0X03, Encoding.ASCII),
        VALID_TO(R.string.cven, 0X04, Encoding.ASCII),
        MUNI_DELIV(R.string.gmun, 0X05),
        RRN(R.string.nanu, 0X06, Encoding.ASCII),
        NAME(R.string.name, 0X07),
        GIVEN_NAME(R.string.give, 0X08, "-"),
        FIRST_CHAR(R.string.thir, 0X09),
        NATIONALITY(R.string.nati, 0X0A),
        BIRTH_LOCATION(R.string.birt, 0X0B),
        BIRTH_DATE(R.string.bird, 0X0C),
        SEX(R.string.gend, 0X0D, Encoding.ASCII),
        NOBLE_CONIDTION(R.string.nobl, 0X0E, ""),
        DOCUMENT_TYPE(R.string.docu, 0X0F, Encoding.ASCII),
        SPECIAL_STATUS(R.string.spec, 0X10, Encoding.ASCII, "0"),
        HASH_PHOTO(R.string.hpic, 0X11, Encoding.BINARY),
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

        Field(int stringId, int tag, Encoding encoding) {
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
