package be.benim.eid;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.widget.TextView;

/**
 * Created by benjamin on 21.03.18.
 */

class EidReader {
    private CCIDReader reader;

    EidReader() {
    }

    void init(UsbManager manager, UsbDevice device) {
        UsbDeviceConnection connection= manager.openDevice(device);
        UsbInterface usbInterface= device.getInterface(0);
        connection.claimInterface(usbInterface, true);
        reader = new CCIDReader(connection, usbInterface);
        reader.run();
    }

    void fetchData(EidData data) {

    }
}
