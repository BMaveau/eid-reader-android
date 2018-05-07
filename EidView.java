package be.benim.eid;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.google.gson.Gson;

public class EidView extends AppCompatActivity {
    static final String INTENT_RESULT= "be.benim.eid.result";
    static final String RESULT_ERROR= "error";
    static final String INTENT_START= "be.benim.eid.start";
    static final String GET_DATA= "data";
    private static final String ACTION_USB_PERMISSION = "be.benim.eid.USB_PERMISSION";

    static  final EidHandler handler= new EidHandler(Looper.getMainLooper());
    private UsbDevice device= null;
    private UsbManager manager= null;
    private EidData data= null;
    private EidReader connection;
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) synchronized (this) {
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED,
                        false))
                    initCommunication();
                else
                    endWithError(Error.NO_PERMISSION);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_eid_view);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(mUsbReceiver, filter);
        parseIntent(getIntent());
    }

    static EidHandler getHandler() {
        return handler;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        parseIntent(intent);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mUsbReceiver);
    }

    /**
     * Checks the given intent. There are three possibilities:
     * USB attached action: Gets the device from the intent and initialises the communication.
     * Start action: Gets the data field from the intent. If device is initialised, starts
     * communication
     * Otherwise: Gets all the data fields from the eid and displays this in the view.
     * @param intent The intent to parse
     */
    private void parseIntent(Intent intent) {
        if (intent.getAction()!= null &&
                intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
            log("CCID reader attached");
            device = getIntent().getParcelableExtra(UsbManager.EXTRA_DEVICE);
            if (device== null)
                log("Intent returns an empty device");
            initCommunication();
        } else {
            String json= EidData.getAll();
            if (intent.getAction()!= null && intent.getAction().equals(INTENT_START))
                json= intent.getStringExtra(GET_DATA);
            data= new Gson().fromJson(json, EidData.class);
            if (device!= null)
                startCommunication();
        }
    }

    /**
     * Initialises the communication to the usb device. If the data field is not empty, the
     * data is extracted from the usb device using startCommunication.
n     */
    private void initCommunication() {
        if (device== null) {
            log("Communication initialised but device is null");
            return;
        }

        if (manager== null) {
            manager = (UsbManager) getSystemService(Context.USB_SERVICE);
            if (manager== null) {
                endWithError(Error.NO_MANAGER);
                return;
            }
        }
        if (!manager.hasPermission(device)) {
            manager.requestPermission(device, PendingIntent.getBroadcast(this, 0,
                    new Intent(ACTION_USB_PERMISSION), 0));
            return;
        }
        connection= new EidReader(this, (TextView) findViewById(R.id.text_log));
        connection.init(manager, device);
        if (data!= null)
            startCommunication();
    }

    private void startCommunication() {
    }

    private void log(String text) {
        ((TextView) findViewById(R.id.text_log)).append("\n" + text);
    }

    void endWithError(Error error) {
        log("End with error requested");
//        Intent intent= new Intent(INTENT_RESULT);
//        intent.putExtra(RESULT_ERROR, error);
//        setResult(RESULT_CANCELED, intent);
//        finish();
    }

    enum Error{
        NO_PERMISSION, NO_MANAGER, MALFORMED
    }
}
