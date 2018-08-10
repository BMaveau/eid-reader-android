package be.benim.eid;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.support.annotation.NonNull;

import org.json.JSONObject;

/**
 * Created by benjamin on 21.03.18.
 */

class EidReader {
    private CCIDReader reader;
    private Context context;
    private EidRequest request;
    private JSONObject result;
    private EidTask task;

    EidReader(@NonNull Context context) {
        this.context = context;
        request = null;
        result = new JSONObject();
        task = null;
    }

    void init(UsbManager manager, UsbDevice device) {
        UsbDeviceConnection connection= manager.openDevice(device);
        UsbInterface usbInterface= device.getInterface(0);
        connection.claimInterface(usbInterface, true);
        reader = new CCIDReader(connection, usbInterface);
        new Thread(reader).start();
    }

    public void setRequest(EidRequest request) {
        this.request = request;
    }

    void next() {
        if (task == null || task.done) {
            task = EidTask.next(context, request, reader, result);
            if (task == null) {
                reader.quitLoop();
                return;
            }
            task.init();
        } else if (!task.run()){
            task.process();
            if (!task.error.isEmpty())
                reader.quitLoop();
            next();
        }
    }

    public JSONObject getResult() {
        return result;
    }

    void handleError() {

    }

}
