package be.benim.eid;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

/**
 * Created by benjamin on 04.05.18.
 */

public class EidHandler extends Handler {
    static final int MES_LOG = 1;
    static final int MES_ERR = 2;
    static final int MES_STA = 3;

    private LogAdded logAdded= null;
    private ErrOccurred errOccurred = null;
    private StateChanged stateChanged = null;

    public EidHandler(Looper looper) {
        super(looper);
    }

    public void setLogAdded(LogAdded logAdded) {
        this.logAdded = logAdded;
    }

    public void setErrOccurred(ErrOccurred errOccurred) {
        this.errOccurred = errOccurred;
    }

    public void setStateChanged(StateChanged stateChanged) {
        this.stateChanged = stateChanged;
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case MES_LOG:
                callLogAdded(msg);
                break;
            case MES_ERR:
                callErrorOccured(msg);
                break;
            case MES_STA:
                callStateChanged(msg);
                break;
        }
        super.handleMessage(msg);
    }

    private void callLogAdded(Message msg) {
        if (logAdded != null)
            logAdded.logAdded((String) msg.obj);
    }

    private void callErrorOccured(Message msg) {
        if (errOccurred != null)
            errOccurred.errOccured((EidView.Error) msg.obj);
    }

    private void callStateChanged(Message msg) {
        if (stateChanged != null) {
            CCIDReader.Status[] statuses = (CCIDReader.Status[]) msg.obj;
            stateChanged.stateChanged(statuses[0], statuses[1]);
        }

    }

    interface LogAdded {
        void logAdded(String log);
    }

    interface ErrOccurred {
        void errOccured(EidView.Error error);
    }

    interface StateChanged {
        void stateChanged(CCIDReader.Status oldStatus, CCIDReader.Status newStatus);
    }
}
