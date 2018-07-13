package be.benim.eid;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by benjamin on 08.07.18.
 */

public class EidRequest implements Parcelable {
    boolean getID;

    static EidRequest getAll() {
        EidRequest eidRequest = new EidRequest();
        eidRequest.getID = true;
        return eidRequest;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(getID ? 1 : 0);
    }

    public static final Parcelable.Creator<EidRequest> CREATOR
            = new Parcelable.Creator<EidRequest>() {
        public EidRequest createFromParcel(Parcel in) {
            EidRequest eidRequest= new EidRequest();
            eidRequest.getID = (in.readInt() == 1);
            return eidRequest;
        }

        public EidRequest[] newArray(int size) {
            return new EidRequest[size];
        }
    };

}
