package be.benim.eid;

import com.google.gson.Gson;

/**
 * Created by benjamin on 23.03.18.
 */

public class EidData {
    String naam= null;
    String voornaam= null;

    EidData() {

    }

    static String getAll() {
        EidData data= new EidData();
        data.naam= "";
        data.voornaam= "";
        return new Gson().toJson(data);
    }

}
