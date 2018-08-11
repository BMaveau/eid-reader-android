package be.benim.eid;

import java.io.ByteArrayOutputStream;

/**
 * Created by benjamin on 18.07.18.
 */

public class BulkSecureVerification extends BulkMessageOut {
    byte bwi = 0X00;
    BulkOutXfg.LevelParameter levelParameter;
    byte timeout = 0X00;
    byte formatString = (byte) 0b10001001;
    byte pinBlockString = 0B01000111;
    byte pinLengthFormat = 0B00000100;
    int minLen = 4;
    int maxLen = 12;
    byte entryValidationCondition = 0X02;
    byte numberMessage = (byte) 0XFF;
    int langId = 0X0813;
    byte[] pinApdu =  new byte[] {0X00, 0X20, 0X00, 0X01, 0X08, 0X20, -1, -1, -1, -1, -1, -1, -1};


    public BulkSecureVerification() {
        super((byte) 0X00, (byte) 0X00);
        type = 0X69;
        levelParameter = BulkOutXfg.LevelParameter.BEG_AND_END;
        length = HelperFunc.intToBytes(28);
    }

    @Override
    protected ByteArrayOutputStream writeMessage() {
        ByteArrayOutputStream stream = super.writeMessage();
        stream.write(bwi);
        stream.write(levelParameter.getValue(), 0, 2);
        stream.write(0X00);
        stream.write(timeout);
        stream.write(formatString);
        stream.write(pinBlockString);
        stream.write(pinLengthFormat);
        stream.write((byte) maxLen);
        stream.write((byte) minLen);
        stream.write(entryValidationCondition);
        stream.write(numberMessage);
        stream.write(HelperFunc.intToByte(langId, 2), 0, 2);
        for (int i = 0; i< 4; i++)
            stream.write(0X00);
        stream.write(pinApdu, 0, pinApdu.length);
        return stream;
    }
}
