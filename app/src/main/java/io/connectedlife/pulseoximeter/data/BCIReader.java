package io.connectedlife.pulseoximeter.data;

import java.util.Vector;

/**
 * Created by bill on 3/7/18.
 *
 *  Simple buffer to read data in and then process every 5 bytes at a time
 */

public class BCIReader {

    private Vector<Byte> mBuffer;

    public BCIReader() {
        mBuffer = new Vector<>();
    }

    // add any number of data bytes to the queue
    public void add(byte[] data) {
        for ( byte value: data) {
            mBuffer.add(value);
        }
    }

    // return true if the first 5 bytes are valid data
    // if the first 5 bytes are invalid then remove the first one to try again
    public boolean hasData() {
        BCIData data = new BCIData();
        byte[] buffer = getDataRecord();
        if ( buffer != null) {
            if ( data.isDataValid(buffer) ) {
                return true;
            }
            else {
                // if no data found then pop the first on off, to re-sync
                popDataFromBuffer(1);
            }
        }
        return false;
    }


    // return a BCIData object from the front of the queue, if no data valid data
    // then return null
    public BCIData getData() {
        BCIData data = new BCIData();
        byte[] buffer = getDataRecord();
        if ( buffer == null) {
            return null;
        }
        if ( data.isDataValid(buffer) ) {
            popDataFromBuffer(buffer.length);
            data.decode(buffer);
            return data;
        }
        return null;
    }


    // return the next 5 bytes as a byte array from the queue
    protected byte[] getDataRecord() {
        byte[] buffer = null;
        if (mBuffer.size() >= BCIData.DATA_LENGTH) {
            buffer = new byte[BCIData.DATA_LENGTH];
            for (int i = 0; i < buffer.length; i++) {
                buffer[i] = mBuffer.get(i);
            }
        }
        return buffer;
    }

    // remove the first <length> bytes from the queue
    protected void popDataFromBuffer(int length) {
        for ( int i = 0; i < length; i ++) {
            mBuffer.remove(0);
        }
    }

}
