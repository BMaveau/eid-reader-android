package be.benim.eid;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.EnumSet;

/**
 * Created by benjamin on 02.04.18.
 */

class CCID {
    private int releasenumber;
    private int nofSlots;
    private int defaultFrequency;
    private int maxFrequency;
    private int noClockFrequencies;
    private int defaultDataRate;
    private int maxDataRate;
    private int noDataRates;
    private int maxLen;
    private boolean interrupt;
    private boolean autoParamaters;
    private boolean autoActivate;
    private boolean autoVolt;
    private boolean autoFrequency;
    private boolean autoDataRate;
    private EnumSet<Voltage> voltages;
    private Exchange exchange;


    CCID(byte[] interfaceDescriptor) {
        setInterrupt(interfaceDescriptor[4] == 0x03);
    }

    void setUsingClassDescriptor(byte[] descriptor) {
        voltages= EnumSet.noneOf(Voltage.class);
        setNofSlots(descriptor[4] + 1);
        setVoltages(descriptor[5]);
        setDefaultFrequency(Arrays.copyOfRange(descriptor, 10, 14));
        setMaxFrequency(Arrays.copyOfRange(descriptor, 14, 18));
        setNoClockFrequencies(descriptor[18]);
        setDefaultDataRate(Arrays.copyOfRange(descriptor, 19, 23));
        setMaxDataRate(Arrays.copyOfRange(descriptor, 23, 27));
        setNoDataRates(descriptor[27]);
        setMaxLen(Arrays.copyOfRange(descriptor, 28, 32));
        setFeatures(Arrays.copyOfRange(descriptor, 40,44));
    }

    void isActive() {

    }

    private void setVoltages(int volt) {
        voltages.clear();
        if (volt % 2 == 1)
            voltages.add(Voltage.FIVE);
        if ((volt >> 1) % 2 == 1)
            voltages.add(Voltage.THREE);
        if ((volt >> 2) % 2 == 1)
            voltages.add(Voltage.ONE_EIGHT);
    }

    public int getNofSlots() {
        return nofSlots;
    }

    public void setNofSlots(int nofSlots) {
        this.nofSlots = nofSlots;
    }

    boolean isInterrupt() {
        return interrupt;
    }

    private void setInterrupt(boolean interrupt) {
        this.interrupt = interrupt;
    }

    public int getDefaultFrequency() {
        return defaultFrequency;
    }

    void setDefaultFrequency(byte[] frequency) {
        this.defaultFrequency = bytesToInt(frequency);
    }

    public int getMaxFrequency() {
        return maxFrequency;
    }

    void setMaxFrequency(byte[] maxFrequency) {
        this.maxFrequency = bytesToInt(maxFrequency);
    }

    public int getNoClockFrequencies() {
        return noClockFrequencies;
    }

    public void setNoClockFrequencies(int noClockFrequencies) {
        this.noClockFrequencies = noClockFrequencies;
    }

    public int getDefaultDataRate() {
        return defaultDataRate;
    }

    public void setDefaultDataRate(byte[] defaultDataRate) {
        this.defaultDataRate = bytesToInt(defaultDataRate);
    }

    public int getMaxDataRate() {
        return maxDataRate;
    }

    public void setMaxDataRate(byte[] maxDataRate) {
        this.maxDataRate = bytesToInt(maxDataRate);
    }

    public int getNoDataRates() {
        return noDataRates;
    }

    public void setNoDataRates(int noDataRates) {
        this.noDataRates = noDataRates;
    }

    int getMaxLen() {
        return maxLen;
    }

    void setFeatures(byte[] features) {
        autoParamaters= HelperFunc.getBit(features[0], 1);
        autoActivate= HelperFunc.getBit(features[0], 2);
        autoVolt= HelperFunc.getBit(features[0], 3);
        autoFrequency= HelperFunc.getBit(features[0], 4);
        autoDataRate= HelperFunc.getBit(features[0], 5);
        if (HelperFunc.getBit(features[2], 0))
            exchange = Exchange.TPDU;
        else if (HelperFunc.getBit(features[2], 1))
            exchange = Exchange.SHORT_APDU;
        else if (HelperFunc.getBit(features[2], 2))
            exchange = Exchange.EX_APDU;
        else
            exchange = Exchange.CHAR;
    }

    void setMaxLen(byte[] maxLen) {
        this.maxLen = bytesToInt(maxLen);
    }

    public boolean isAutoParamaters() {
        return autoParamaters;
    }

    public boolean isAutoActivate() {
        return autoActivate;
    }

    public boolean isAutoVolt() {
        return autoVolt;
    }

    public boolean isAutoFrequency() {
        return autoFrequency;
    }

    public boolean isAutoDataRate() {
        return autoDataRate;
    }

    String printInfo() {
        return "Number of slots: " + nofSlots + "\n" +
                "Default frequency: " + defaultFrequency + "\n" +
                "Maximum frequency: " + maxFrequency + "\n" +
                "Number of frequencies: " + noClockFrequencies + "\n" +
                "Default data rate: " + defaultDataRate + "\n" +
                "Maximum data rate: " + maxDataRate + "\n" +
                "Number of data rates: " + noDataRates + "\n" +
                "Maximum lenght: " + maxLen + "\n" +
                "Interrupt endpoint: " + interrupt + "\n" +
                "Automatic parameters: " + autoParamaters + "\n" +
                "Automatic active ICC: " + autoActivate + "\n" +
                "Automatic voltage: " + autoVolt + "\n" +
                "Automatic frequency: " + autoFrequency + "\n" +
                "Automatic data rate: " + autoDataRate + "\n" +
                "Supported voltages: " + voltages + "\n" +
                "Data exchange: " + exchange.toString();

    }

    static int bytesToInt(byte[] bytes) {
        ByteBuffer byteBuffer= ByteBuffer.wrap(bytes);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        return byteBuffer.getInt();
    }


    enum Voltage {
        FIVE, THREE, ONE_EIGHT;

        @Override
        public String toString() {
            if (this == FIVE)
                return "5 V";
            if (this == THREE)
                return "3 V";
            if (this == ONE_EIGHT)
                return "1.8 V";
            else
                return "Not a voltage";
        }
    }

    enum Exchange {
        TPDU, SHORT_APDU, EX_APDU, CHAR;

        @Override
        public String toString() {
            switch (this) {
                case TPDU: return "TPDU";
                case EX_APDU:return "Extended APDU";
                case SHORT_APDU:return "Short APDU";
                case CHAR: return "Character exchange";
                default:return  "Should not happen";
            }
        }
    }
}
