package be.benim.eid;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

/**
 * Created by benjamin on 08.07.18.
 */

public class EidRequest implements Parcelable {
    boolean getID;
    boolean getAddress;
    boolean getPic;
    boolean checkID;
    boolean checkAddress;
    boolean checkPic;
    boolean getCertAuth;
    boolean getCertNonRep;
    boolean getCertCA;
    boolean getCertRoot;
    private ArrayList<ToEncrypt> toEncrypt;

    public EidRequest() {
        toEncrypt = new ArrayList<>();
    }

    static EidRequest getAll() {
        EidRequest eidRequest = new EidRequest();
        eidRequest.getID = true;
        eidRequest.getAddress = true;
        eidRequest.getPic = true;
        eidRequest.checkID = true;
        eidRequest.checkAddress = true;
        eidRequest.checkPic = true;
        eidRequest.getCertAuth = true;
        eidRequest.getCertNonRep = true;
        eidRequest.getCertCA = true;
        eidRequest.getCertRoot = true;
        try {
            byte[] testMessage = "Dit is een test".getBytes();
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] messageSha1 = digest.digest(testMessage);
            eidRequest.addDataToEncrypt(new ToEncrypt(messageSha1, Algorithm.RSA_PKCS1_SHA1,
                    true));
//            eidRequest.addDataToEncrypt(new ToEncrypt(messageSha1, Algorithm.RSA_PKCS1_SHA1,
//                    false));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return eidRequest;
    }

    int addDataToEncrypt(@NonNull ToEncrypt toEncrypt) {
        this.toEncrypt.add(toEncrypt);
        toEncrypt.index = this.toEncrypt.size() - 1;
        return this.toEncrypt.size() - 1;
    }

    public ArrayList<ToEncrypt> getToEncrypt() {
        return toEncrypt;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(getID ? 1 : 0);
        parcel.writeInt(getAddress ? 1 : 0);
        parcel.writeInt(getPic ? 1 : 0);
        parcel.writeInt(checkID ? 1 : 0);
        parcel.writeInt(checkAddress ? 1 : 0);
        parcel.writeInt(checkPic ? 1 : 0);
        parcel.writeInt(getCertAuth ? 1 : 0);
        parcel.writeInt(getCertNonRep ? 1 : 0);
        parcel.writeInt(getCertCA ? 1 : 0);
        parcel.writeInt(getCertRoot ? 1 : 0);
        parcel.writeSerializable(toEncrypt);
    }

    public static final Parcelable.Creator<EidRequest> CREATOR
            = new Parcelable.Creator<EidRequest>() {
        public EidRequest createFromParcel(Parcel in) {
            EidRequest eidRequest= new EidRequest();
            eidRequest.getID = (in.readInt() == 1);
            eidRequest.getAddress = (in.readInt() == 1);
            eidRequest.getPic = (in.readInt() == 1);
            eidRequest.checkID = (in.readInt() == 1);
            eidRequest.checkAddress = (in.readInt() == 1);
            eidRequest.checkPic = (in.readInt() == 1);
            eidRequest.getCertAuth = (in.readInt() == 1);
            eidRequest.getCertNonRep = (in.readInt() == 1);
            eidRequest.getCertCA = (in.readInt() == 1);
            eidRequest.getCertRoot = (in.readInt() == 1);
            eidRequest.toEncrypt = in.readArrayList(null);
            return eidRequest;
        }

        public EidRequest[] newArray(int size) {
            return new EidRequest[size];
        }
    };

    static class ToEncrypt implements Parcelable, Comparable {
        boolean authentication;
        byte[] toEncrypt;
        private int index;
        Algorithm algorithm;

        public ToEncrypt() {
        }

        public ToEncrypt(byte[] toEncrypt, Algorithm algorithm, boolean authentication) {
            this.authentication = authentication;
            this.toEncrypt = toEncrypt;
            this.algorithm = algorithm;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(authentication ? 1 : 0);
            parcel.writeByteArray(toEncrypt);
            parcel.writeSerializable(algorithm);
        }

        public static final Parcelable.Creator<ToEncrypt> CREATOR
                = new Parcelable.Creator<ToEncrypt>() {
            public ToEncrypt createFromParcel(Parcel in) {
                ToEncrypt toEncrypt = new ToEncrypt();
                toEncrypt.authentication = (in.readInt() == 1);
                in.readByteArray(toEncrypt.toEncrypt);
                toEncrypt.algorithm = (Algorithm) in.readSerializable();
                return toEncrypt;
            }

            public ToEncrypt[] newArray(int size) {
                return new ToEncrypt[size];
            }
        };

        int getInt() {
            return (authentication ? 1 : 0) * 100000 + algorithm.value * 100 + index;
        }

        public int getIndex() {
            return index;
        }

        public boolean sameMethod(ToEncrypt toEncrypt) {
            return toEncrypt.authentication == authentication && toEncrypt.algorithm == algorithm;
        }

        @Override
        public int compareTo(@NonNull Object o) {
            if (o instanceof ToEncrypt)
                return getInt() - ((ToEncrypt) o).getInt();
            return 0;
        }
    }

    enum Algorithm {
        RSA_PKCS1(0X01),
        RSA_PKCS1_SHA1(1 << 1),
        RSA_PKCS1_MD5(1 << 2),
        RSA_PKCS1_SHA256(1 << 3),
        RSA_PSS_SHA1(1 << 4),
        RSA_PSS_SHA256(1 << 5);

        byte value;

        Algorithm(int value) {
            this.value = (byte) value;
        }
    }
}
