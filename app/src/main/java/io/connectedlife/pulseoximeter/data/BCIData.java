package io.connectedlife.pulseoximeter.data;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Locale;

/**
 * Created by bill on 3/7/18.
 *
 * Decode and store 5 bytes data from the BloodOximeter
 */

public class BCIData implements Parcelable {
    private int mSignalStrength;
    private int mPleth;
    private int mBargraph;
    private boolean mIsFingerPresent;
    private boolean mIsPulseFound;
    private boolean mIsPulseBeep;
    private boolean mIsProbePlugged;
    private boolean mIsSignal;
    private int mPulseRate;
    private int mSpO2;

    public static final int DATA_LENGTH = 5;

    public BCIData() {
        mSignalStrength = 0;
        mPleth = 0;
        mBargraph = 0;
        mPulseRate = 0;
        mSpO2 = 0;
        mIsFingerPresent = false;
        mIsPulseFound = false;
        mIsProbePlugged = false;
        mIsSignal = false;
        mIsPulseBeep = false;
    }

    public boolean isDataValid(byte[] data) {
        if ( data == null) {
            return false;
        }
        if ( data.length != DATA_LENGTH) {
            return false;
        }
        if ( (data[0] & 0x80 ) != 0x80 ) {
            return false;
        }
        for ( int i = 1; i < data.length; i ++) {
            if ( (data[i] & 0x80 ) != 0x00 ) {
                return false;
            }
        }
        return true;
    }

    public boolean decode(byte[] data) {

        // check for the header bit set
        if ( ! isDataValid(data)) {
            return false;
        }

        if ( (data[0] & 0x80 ) == 0x80 ) {
            mSignalStrength = (data[0] & 0x0F);
            mIsSignal = ((data[0] & 0x10) != 0x10);
            mIsProbePlugged = ((data[0] & 0x20) != 0x20);
            mIsPulseBeep = ((data[0] & 0x40) == 0x40);
        }
        if ( (data[1] & 0x80) == 0x00 ) {
            mPleth = (data[1] & 0x7F);
        }
        if ( (data[2] & 0x80) == 0x00 ) {
            mBargraph = (data[2] & 0x0F);
            mIsFingerPresent = ((data[2] & 0x10) != 0x10);
            mIsPulseFound = ((data[2] & 0x20) != 0x20);
        }
        if ( (data[3] & 0x80) == 0x00 ) {
            mPulseRate = ((data[2] & 0x40) << 1) | ( data[3] & 0x7F);
        }
        if ( (data[4] & 0x80) == 0x00 ) {
            mSpO2 = (data[4] & 0x7F);
        }
        return true;
    }

    protected BCIData(Parcel in) {
        mSignalStrength = in.readInt();
        mPleth = in.readInt();
        mBargraph = in.readInt();
        mPulseRate = in.readInt();
        mSpO2 = in.readInt();
        mIsFingerPresent = in.readByte() != 0;
        mIsPulseFound = in.readByte() != 0;
        mIsProbePlugged = in.readByte() != 0;
        mIsSignal = in.readByte() != 0;
        mIsPulseBeep = in.readByte() != 0;
    }

    public static final Creator<BCIData> CREATOR = new Creator<BCIData>() {
        @Override
        public BCIData createFromParcel(Parcel in) {
            return new BCIData(in);
        }

        @Override
        public BCIData[] newArray(int size) {
            return new BCIData[size];
        }
    };

    public int getSignalStrength() { return mSignalStrength;   }
    public int getPleth() { return mPleth; }
    public int getBargraph() { return mBargraph;}
    public int getPulseRate() { return mPulseRate;}
    public int getSpO2() { return mSpO2;}
    public boolean isBargraphValid() { return mBargraph > 0;}
    public boolean isPlethValid() { return mPleth > 0;}
    public boolean isFingerPresent() { return mIsFingerPresent;}
    public boolean isPulseFound() { return mIsPulseFound;}
    public boolean isProbePlugged() { return mIsProbePlugged;}
    public boolean isSignal() { return mIsSignal;}
    public boolean isPulseBeep() { return mIsPulseBeep;}


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

        dest.writeInt(mSignalStrength);
        dest.writeInt(mPleth);
        dest.writeInt(mBargraph);
        dest.writeInt(mPulseRate);
        dest.writeInt(mSpO2);
        dest.writeByte((byte) (mIsFingerPresent ? 1 : 0));
        dest.writeByte((byte) (mIsPulseFound ? 1 : 0));
        dest.writeByte((byte) ( mIsProbePlugged ? 1: 0));
        dest.writeByte((byte) ( mIsSignal ? 1: 0));
        dest.writeByte((byte) ( mIsPulseBeep ? 1: 0));
    }

    @Override
    public String toString() {
        StringBuffer states = new StringBuffer();
        states.append(mIsFingerPresent ? "Finger" : "NoFinger");
        states.append(" ");
        states.append(mIsPulseFound ? "Pulse" : "NoPulse");
        states.append(" ");
        states.append(mIsProbePlugged ? "Plugged" : "NoPlugged");
        states.append(" ");
        states.append(mIsSignal ? "Signal" : "NoSignal");
        states.append(" ");
        states.append(mIsPulseBeep ? "Beep": "");

        return String.format(Locale.UK, "Sig:%d Plth:%d Bar:%d Pulse:%d SpQ2:%d %s", mSignalStrength, mPleth, mBargraph, mPulseRate, mSpO2, states.toString());
    }
}
