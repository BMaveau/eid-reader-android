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

import org.json.JSONException;
import org.json.JSONObject;

public class EidView extends AppCompatActivity {
    static final String INTENT_RESULT= "be.benim.eid.result";
    static final String RESULT_ERROR= "error";
    static final String INTENT_START= "be.benim.eid.start";
    static final String GET_DATA= "data";
    private static final String ACTION_USB_PERMISSION = "be.benim.eid.USB_PERMISSION";

    static  EidHandler handler;
    private UsbDevice device= null;
    private UsbManager manager= null;
    private EidRequest data= null;
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
        handler= new EidHandler(Looper.getMainLooper());
        handler.setLogAdded(new EidHandler.LogAdded() {
            @Override
            public void logAdded(String log) {
                EidView.this.log(log);
            }
        });
        log("Activity created.");
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
        try {
            unregisterReceiver(mUsbReceiver);
        }
        catch (IllegalArgumentException ignored) {

        }
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
            device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            if (device== null)
                log("Intent returns an empty device");
            initCommunication();
        } else {
            if (intent.getAction() != null && intent.getAction().equals(INTENT_START))
                data = intent.getParcelableExtra(GET_DATA);
            else
                data = EidRequest.getAll();
            startCommunication();
        }
    }

    /**
     * Initialises the communication to the usb device. If the data field is not empty, the
     * data is extracted from the usb device using startCommunication.
     */
    private void initCommunication() {
        manager = manager == null ? (UsbManager) getSystemService(Context.USB_SERVICE): manager;
        if (device== null)
            log("Communication initialised but device is null");
        else if(manager == null)
            endWithError(Error.NO_MANAGER);
        else if (!manager.hasPermission(device)) {
            manager.requestPermission(device, PendingIntent.getBroadcast(this, 0,
                    new Intent(ACTION_USB_PERMISSION), 0));
        }
        else {
            connection= new EidReader(this);
            startCommunication();
        }
    }

    private void startCommunication() {
        if (connection!= null && data!= null) {
            connection.setRequest(data);
            handler.setStateChanged(new EidHandler.StateChanged() {
                @Override
                public void stateChanged(CCIDReader.Status oldStatus, CCIDReader.Status newStatus) {
                    if ((oldStatus == CCIDReader.Status.INITIALISING ||
                            oldStatus == CCIDReader.Status.COMMUNICATING) &&
                            newStatus== CCIDReader.Status.IDLE)
                        connection.next();
                    else if (newStatus == CCIDReader.Status.ERROR)
                        connection.handleError();
                    else if (newStatus == CCIDReader.Status.QUIT)
                        end();
                }
            });
            connection.init(manager, device);
        }
    }

    void end() {
        JSONObject object = connection.getResult();
        try {
            log(object.toString(2));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    void log(String text) {
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
        NO_PERMISSION, NO_MANAGER, MALFORMED, IO_ERROR
    }
}
