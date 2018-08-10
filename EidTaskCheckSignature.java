package be.benim.eid;

import android.content.Context;
import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * Created by benjamin on 16.07.18.
 */

public class EidTaskCheckSignature extends EidTaskRead {
    boolean checkID;
    boolean checkAddress;
    boolean checkPic;

    EidTaskCheckSignature(Context context, CCIDReader reader, JSONObject result,
                          EidRequest request) {
        super(context, reader, result, 0XDF00, 0X503C);
        checkID = request.checkID;
        checkAddress = request.checkAddress;
        checkPic = request.checkPic;
        name = "Cert#8(RN)";
    }

    @Override
    void process() {
        super.process();
        try {
            X509Certificate cert = (X509Certificate) CertificateFactory.getInstance("X.509")
                    .generateCertificate(new ByteArrayInputStream(response.toByteArray()));
            Signature signature = Signature.getInstance("SHA1withRSA");
            signature.initVerify(cert.getPublicKey());
            if (checkAddress) {
                signature.update(Base64.decode(result.getString("GetAdd"), Base64.DEFAULT));
                signature.update(Base64.decode(result.getString("SignId"), Base64.DEFAULT));
                result.put("CheckAdd", signature.verify(
                        Base64.decode(result.getString("SignAdd"), Base64.DEFAULT)));
            }
            if (checkID || checkPic) {
                signature.update(Base64.decode(result.getString("GetId"), Base64.DEFAULT));
                result.put("CheckId", signature.verify(
                        Base64.decode(result.getString("SignId"), Base64.DEFAULT)));
            }
            if (checkPic) {
                if (!result.getBoolean("CheckId"))
                    result.put("CheckPic", false);
                else {
                    MessageDigest digest = MessageDigest.getInstance("SHA-1");
                    byte[] signBytes = digest.digest(Base64.decode(
                            result.getString("GetPic"), Base64.DEFAULT));
                    String signPic = HelperFunc.bytesToHex(signBytes);
                    String hashPic = result.getJSONObject("ID").getString(context.getString(
                            EidTaskGetID.Field.HASH_PHOTO.stringId));
                    ((EidView) context).log("sign pic: " + signPic);
                    ((EidView) context).log("hash pic: " + hashPic);
                    result.put("CheckPic", signPic.equals(hashPic));
                }
            }
        } catch (CertificateException e) {
            e.printStackTrace();
            ((EidView) context).log("Error generating certificate: " + e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            ((EidView) context).log("Wrong algorithm: " + e.getMessage());
        } catch (JSONException e) {
            e.printStackTrace();
            ((EidView) context).log("Error getting String from JSON: " + e.getMessage());
        } catch (SignatureException e) {
            e.printStackTrace();
            ((EidView) context).log("Error with signature or parsing: " + e.getMessage());
        } catch (InvalidKeyException e) {
            e.printStackTrace();
            ((EidView) context).log("Public key is invalid: " + e.getMessage());
        }

    }
}
