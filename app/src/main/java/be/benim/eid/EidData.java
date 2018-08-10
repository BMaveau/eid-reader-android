package be.benim.eid;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.StringRes;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.EnumSet;

/**
 * Created by benjamin on 23.03.18
 */

public class EidData implements Parcelable {
    private EnumSet<Field> wantedFields;
    private JSONObject result;

    EidData() {
        result = new JSONObject();
    }

    public EnumSet<Field> getWantedFields() {
        return wantedFields;
    }

    public void setWantedFields(EnumSet<Field> wantedFields) {
        this.wantedFields = wantedFields;
    }

    public JSONObject getResult() {
        return result;
    }

    static EidData getAll() {
        EidData data= new EidData();
        data.setWantedFields(EnumSet.allOf(Field.class));
        return data;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeSerializable(wantedFields);
        parcel.writeString(result.toString());
    }

    public static final Parcelable.Creator<EidData> CREATOR
            = new Parcelable.Creator<EidData>() {
        public EidData createFromParcel(Parcel in) {
            EidData eidData = new EidData();
            try {
                Serializable serializable =  in.readSerializable();
                eidData.setWantedFields((EnumSet<Field>) serializable);
                eidData.result = new JSONObject(in.readString());
            }
            catch (JSONException e) {
                throw new RuntimeException(e);
            }
            return eidData;
        }

        public EidData[] newArray(int size) {
            return new EidData[size];
        }
    };

    enum Field {
        FILE_STRUC(R.string.stru, 0x4031, 0x00, Encoding.BINARY, 0),
        CART_NUM(R.string.canu, 0X4031, 0X01, Encoding.ASCII),
        CHIP_NIM(R.string.chnu, 0X4031, 0X02, Encoding.BINARY),
        VALID_FROM(R.string.cvbe, 0X4031, 0X03, Encoding.ASCII),
        VALID_TO(R.string.cven, 0X4031, 0X04, Encoding.ASCII),
        MUNI_DELIV(R.string.gmun, 0X4031, 0X05),
        RRN(R.string.nanu, 0X4031, 0X06, Encoding.ASCII),
        NAME(R.string.name, 0X4031, 0X07),
        GIVEN_NAME(R.string.give, 0X4031, 0X08, "-");
        int stringId;
        byte[] fileAddress;
        int len;
        byte tag;
        Encoding encoding;
        Object standard;

        Field(int stringId, int fileAddress, int tag, Encoding encoding, Object standard) {
            this.stringId = stringId;
            this.fileAddress = HelperFunc.intToByte(fileAddress, 2);
            this.tag = (byte) tag;
            this.encoding = encoding;
            this.standard = standard;
        }

        Field(@StringRes int stringId, int fileAddress, int tag, Object standard) {
            this(stringId, fileAddress, tag, Encoding.UTF8, standard);
        }

        Field(int stringId, int fileAddress, byte tag, Encoding encoding) {
            this(stringId, fileAddress, tag, encoding, null);
        }

        Field(@StringRes int stringId, int fileAddress, int tag) {
            this(stringId, fileAddress, tag, null);
        }
    }

    enum Encoding {
        BINARY, ASCII, UTF8
    }



}
