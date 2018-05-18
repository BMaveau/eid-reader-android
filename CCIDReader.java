package be.benim.eid;

import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Class that initialises the CCID and sends message to it. This class assumes no knowledge about
 * the smart card.
 * Created by benjamin on 17.04.18.
 */

public class CCIDReader implements Runnable {
    /**
     * Status field of the CCID Reader. Possibilities are:
     * - INITIALISING -> Detecting the properties of the CCID
     * - IDLE -> No messages to send
     * - WAITING -> Waiting for Smart Card
     * - COMMUNICATING -> Communicating with device
     * - ERROR -> an error occurred during communication with smart card.
     */
    private Status status= Status.IDLE;
    /**
     * The CCID device connected to the Android device
     */
    private CCID ccid= null;
    /**
     * The messages to send to the CCID.
     */
    private ArrayList<BulkMessageOut> messagesToSend;
    /**
     *The message received from the CCID.
     */
    private ArrayList<BulkMessageIn> messagesReceived;
    /**
     * Length equals the number of slots in the CCID. If value is zero, the slot contains an active
     * smart card. If one, the smart card is inactive. If the value is two, there's no smart
     * card present
     */
    private int[] smartCards;
    private byte activeSlot = -1;
    /**
     * If slot "activeSlot" contains a smart card, the messages in "messagesToSend" are sent until
     * the message at index maxIndex (not included). A value of -1 corresponds to all messages.
     */
    private int maxIndex = -1;
    /**
     * The index after the last message in messagesToSend that was sent.
     */
    private int lastIndex = 0;
    private int timeout = 1000;
    /**
     * The sequence number of the last message that was send.
     */
    private byte lastSequence = -1;
    private UsbEndpoint endBulkOut = null;
    private UsbEndpoint endBulkIn =  null;
    private UsbEndpoint endIntIn = null;
    private UsbInterface usbInterface = null;
    private UsbDeviceConnection connection = null;
    private String log = "";
    private EidView.Error criticalError = null;

    public CCIDReader(UsbDeviceConnection connection, UsbInterface usbInterface) {
        messagesReceived = new ArrayList<>();
        messagesToSend = new ArrayList<>();
        this.usbInterface = usbInterface;
        this.connection = connection;
        parseDescriptors(connection.getRawDescriptors());
        if (status != Status.ERROR_CRITICAL ) {
            smartCards = new int[ccid.getNofSlots()];
            Arrays.fill(smartCards, 2);
        }
    }

    /**
     * Adds a message to te queue of messages. The sequence number and the slot number (if active
     * slot is not -2) is set by this class.
     * @return The index of this message
     */
    int addMessageToQueue(BulkMessageOut mess) {
        messagesToSend.add(mess);
        return messagesToSend.size() - 1;
    }

    /**
     * Returns the message at the given index.
     * @param index The index of the requested message.
     * @return The message asked for or null if non existing.
     */
    @Nullable
    BulkMessageIn getMessage(int index) {
        if (index >= 0 && index < messagesReceived.size())
            return messagesReceived.get(index);
        return null;
    }

    public EidView.Error getCriticalError() {
        return criticalError;
    }

    String getLog() {
        return log;
    }

    /**
     * The currently active slot. The possibilities are:
     * - value between 0 and number of slots (not included): the slot currently used for
     * communication
     * - -1 The first smart card that is found is used. Once a card has been found, this value is
     * altered to the slot number of the card
     * - -2 This class does not alter the slot number of the messages
     * @return The active slot number.
     */
    int getActiveSlot() {
        return activeSlot;
    }

    /**
     * The slot currently used in communication. A value of -1 corresponds to the first slot which
     * contains a smart card. If the value is lower than -1, the slot number is not altered.
     * @param activeSlot The new active slot
     */
    void setActiveSlot(byte activeSlot) {
        if (activeSlot!= -1 && activeSlot >= smartCards.length)
            return;
        this.activeSlot = activeSlot;
    }

    public int getMaxIndex() {
        return maxIndex;
    }

    public int getTimeout() {
        return timeout;
    }

    @Override
    public void run() {
        getStatus();
        while (status != Status.ERROR_CRITICAL && status != Status.QUIT) {
            if (isSmartCardPresent()) {
                if (smartCards[activeSlot] == 1)
                    powerOn();
                int end = maxIndex == -1 ? messagesToSend.size() : maxIndex;
                for (int i = lastIndex; i < end; i++)
                    messagesReceived.add(i, sendMessage(messagesToSend.get(i)));
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                log("Sleep interrupted");
            }
        }
    }

    /**
     * Calls the powerOn message to the current activeSlot.
     */
    public void powerOn() {
        powerOn(activeSlot);
    }

    /**
     * Sends the powerOn message to the smart card in the given slot.
     * @param slot A number between zero and the number of slots (not included)
     */
    public void powerOn(int slot) {
        if (slot < 0 || slot >= ccid.getNofSlots()) {
            log("The given slot (" + Integer.toString(slot) + ") is smaller than zero or is " +
                    "than the number of slots present (" + Integer.toString(ccid.getNofSlots()) +
                    ").");
            return;
        }
        BulkMessageIn response = sendMessage(new BulkOutPowerOn((byte) slot, (byte) 0x00));
        if (response == null)
            return;
        log(response.toString());
    }

    private void parseDescriptors(byte[] descriptors) {
        setStatus(Status.INITIALISING);
        int index= descriptors[0];
        if (index> descriptors.length || descriptors[index] + index> descriptors.length)
            setCriticalError(EidView.Error.MALFORMED);
        log("Device descriptor: " + HelperFunc.bytesToHex(Arrays.copyOfRange(
                descriptors, 0, index)));
        log("Configuration descriptor: " + HelperFunc.bytesToHex(Arrays.copyOfRange(
                descriptors, index, index+descriptors[index])));
        index+= descriptors[index];
        if (index+9 > descriptors.length)
            setCriticalError(EidView.Error.MALFORMED);
        parseInterfaceDescriptor(Arrays.copyOfRange(descriptors, index, index+9));
        index+= descriptors[index];
        if (index+36 > descriptors.length)
            setCriticalError(EidView.Error.MALFORMED);
        parseDeviceClassDescriptor(Arrays.copyOfRange(descriptors, index, index+54));
        for (index += 54; index < descriptors.length; index += descriptors[index]) {
            parseEndpointDescriptor(Arrays.copyOfRange(descriptors, index,
                    index+descriptors[index]));
        }
        setStatus(Status.WAITING);
    }


    /**
     * Checks if the usb interface descriptor matches the required format. If correct, extracts the
     * number of endpoins (2 or 3). Ohterwise the connection is stopped.
     * @param descriptor the byte array descriptor.
     */
    private void parseInterfaceDescriptor(byte[] descriptor) {
        log("Interface descriptor: " + HelperFunc.bytesToHex(descriptor));
        if (descriptor[0]!= 0x09 || descriptor[1]!= 0x04 || descriptor[5]!= 0x0B
                || descriptor[6]!= 0x00 || descriptor[7]!= 0x00) {
            log("Interface descriptor malformed.");
            setCriticalError(EidView.Error.MALFORMED);
        } else
            ccid= new CCID(descriptor);
    }

    /**
     * Checks if the usb device class descriptor matches the required format. If correct, the info
     * is given to the CCID class. Otherwise the connection is stopped.
     * @param descriptor the byte array descriptor.
     */
    private void parseDeviceClassDescriptor(byte[] descriptor) {
        log("Device class descriptor: " + HelperFunc.bytesToHex(descriptor));
        if (descriptor[0]!= 0x36 || descriptor[1] != 0x21) {
            log("Device class descriptor malformed");
            setCriticalError(EidView.Error.MALFORMED);
        } else
            ccid.setUsingClassDescriptor(descriptor);
        log(ccid.printInfo());
    }

    /**
     * Checks if the given endpoint descriptor matches the required format. If correct, it's
     * assigned to the correct UsbEndpoint variable. Otherwise the connection is stopped.
     * @param descriptor te byte array descriptor.
     */
    private void parseEndpointDescriptor(byte[] descriptor) {
        log("Endpoint descriptor: " + HelperFunc.bytesToHex(descriptor));
        if (descriptor[0]!= 0x07 || descriptor[1] != 0x05) {
            log("Endpoint descriptor malformed");
            setCriticalError(EidView.Error.MALFORMED);
        } else {
            int point= descriptor[2];
            if ((point & 0x7F) > usbInterface.getEndpointCount()) {
                log("Endpoint descriptor malformed: The endpoint number is too large: " +
                        HelperFunc.byteToHex(descriptor[2]) + ", " + Integer.toString(point));
                setCriticalError(EidView.Error.MALFORMED);
            }
            else if (descriptor[3] == 0x03 && point < 0)
                endIntIn = usbInterface.getEndpoint((point & 0x7F) - 1);
            else if (descriptor[3] == 0x02) {
                if (point > 0)
                    endBulkOut = usbInterface.getEndpoint((point & 0x7F) - 1);
                else
                    endBulkIn = usbInterface.getEndpoint((point & 0x7F) - 1);
            }
            else {
                log("Endpoint descriptor malformed");
                setCriticalError(EidView.Error.MALFORMED);
            }
        }
    }

    private void log(String message) {
        log += "\n" + message;
        Log.i("Eid", message);
        EidView.getHandler().obtainMessage(EidHandler.MES_LOG, message).sendToTarget();
    }

    private void setCriticalError(EidView.Error error) {
        setStatus(Status.ERROR_CRITICAL);
        criticalError = error;
    }

    /**
     * Sends a message to the CCID. The sequence number is automatically assigned. Returns
     * the response message. The timeout is determined by the corresponding value.
     * @param messageOut The message to send to the CCID. The sequence number is set in this
     *                   function
     * @return The response of the CCID, or null in case of an error.
     */
    @Nullable
    private BulkMessageIn sendMessage(BulkMessageOut messageOut) {
        if (status == Status.ERROR || status == Status.ERROR_CRITICAL)
            return null;

        setStatus(Status.COMMUNICATING);
        messageOut.sequence = ++lastSequence;
        byte[] mess = messageOut.getMessage();
        int sent = connection.bulkTransfer(endBulkOut, mess, mess.length, timeout);
        if (sent != mess.length) {
            status = Status.ERROR_CRITICAL;
            log("Error sending message: " + Integer.toString(sent));
            return null;
        }
        log("Sending message: " + HelperFunc.bytesToHex(mess));
        return receiveMessage();
    }

    @Nullable
    private BulkMessageIn receiveMessage() {
        byte[] header = new byte[10];
        int recv = connection.bulkTransfer(endBulkIn, header, 10, timeout);
        if (recv != 10) {
            status = Status.ERROR_CRITICAL;
            log("Error receiving header message: " + Integer.toString(recv));
            return null;
        }
        log("Received message header: " + HelperFunc.bytesToHex(header));
        BulkMessageIn mess = new BulkMessageIn(header);
        int len = HelperFunc.bytesToInt(mess.length);
        if (len != 0) {
            header = new byte[len];
            recv = connection.bulkTransfer(endBulkIn, header, len, timeout);
            if (recv != len) {
                status = Status.ERROR_CRITICAL;
                log("Error receiving extra part message: " + Integer.toString(recv));
            }
            mess.extra = header;
            log("Received message extra: " + HelperFunc.bytesToHex(header));
        }
        setStatus(Status.WAITING);
        return mess;
    }

    /**
     * Sends status messages to all slots and sets the response to the variable smartcards
     * @return True if a smart card is present and powered in any slot.
     */
    private boolean getStatus() {
        if (status == Status.ERROR_CRITICAL || status == Status.ERROR)
            return false;
        boolean found = false;
        for (int i= 0; i < ccid.getNofSlots(); i++) {
            BulkOutSlot mess = new BulkOutSlot((byte) i, (byte) 0x00);
            BulkMessageIn rec = sendMessage(mess);
            if (rec != null && rec.type == -1 && rec.slot == (byte) i) {
                smartCards[i] = rec.getSmartCardStatus();
                found |= smartCards[i] == 0;
            } else {
                log("The received message is null or the type or slot is malformed.");
                setCriticalError(EidView.Error.MALFORMED);
            }
        }
        return found;
    }

    private boolean isSmartCardPresent() {
        if (ccid.isInterrupt()) {
            listenInterrupt();
            if (activeSlot == -1) {
                for (int i = 0; i < smartCards.length; i++) {
                    int state = smartCards[i];
                    if (state == 1 || state == 0) {
                        activeSlot = (byte) i;
                        return true;
                    }
                }
                return false;
            } else {
                return smartCards[activeSlot] == 0 || smartCards[activeSlot] == 1;
            }
        }
        else
            return getStatus();
    }

    private void listenInterrupt() {
        if (status == Status.ERROR_CRITICAL || status == Status.ERROR)
            return;
        byte[] mess = new byte[1];
        setStatus(Status.COMMUNICATING);
        int recv = connection.bulkTransfer(endIntIn, mess, 1, timeout);
        if (recv == 1) {
            if (mess[0] == 0x50) {
                int length = ccid.getNofSlots() % 4 + 1;
                mess = new byte[length];
                recv =  connection.bulkTransfer(endIntIn, mess, length, timeout);
                if (recv != 4) {
                    log("The received message is too short.\nThe number of slots is: " +
                            Integer.toString(ccid.getNofSlots()) + "\nThe received message has " +
                            "length: " + Integer.toString(length));
                    status = Status.ERROR;
                    return;
                }
                alterState(mess);
            } else if (mess[0] == 0x51) {
                mess = new byte[3];
                recv = connection.bulkTransfer(endIntIn, mess, 3, timeout);
                if (recv != 3) {
                    log("The received hardware error message is too short:\n" +
                            Integer.toString(recv));
                    status = Status.ERROR;
                    return;
                }
                log("The message with sequence " + Byte.toString(mess[1]) + " sent to slot "
                        + Byte.toString(mess[0]) + " has caused a hardware error.");
                status = Status.ERROR;
            }
            else {
                log("Unknown message received.");
                status = Status.ERROR;
            }
        }
        setStatus(Status.WAITING);
    }

    /**
     * Translates an array of bytes to the variable smartcards. If the state of a slot is changed,
     * the corresponding value in smartcards is changed. For simplicity it's assumed that a newly
     * present smart card is powered off.
     * @param states one or more byes formed as the SlotICCState field.
     */
    private void alterState(byte[] states) {
        for (int i = 0; i < states.length; i++) {
            byte state = states[i];
            for (int j = 0; j < 4 && (i * 4 + j) < smartCards.length; j+=2) {
                if (HelperFunc.getBit(state, j + 1))
                    smartCards[i * 4 + j] = HelperFunc.getBit(state, j) ? 1 : 2;
            }
        }
    }

    private void setStatus(Status status) {
        this.status = status;
        EidView.handler.obtainMessage(EidHandler.MES_STA, status).sendToTarget();
    }

    private void setError(int number, String message) {

    }

    enum Status {
        INITIALISING, IDLE, WAITING, COMMUNICATING, ERROR, ERROR_CRITICAL, QUIT
    }
}
