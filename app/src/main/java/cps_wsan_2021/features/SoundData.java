package cps_wsan_2021.features;

import java.util.Arrays;

import cps_wsan_2021.ConfigConst;

public class SoundData {
    private int mSoundWindSize = 16000 * ConfigConst.SOUND_DATA_SIZE;
    private int mSlideWindSize = 8000 * ConfigConst.SOUND_DATA_SIZE;
    private int mSampleRate = 16000;
    private byte[] mData;
    private int mcount = 0;
    private boolean m1stTime = true;

    public SoundData(int sampleRate, int windSize_ms, int strideSize_ms) {
        mSampleRate = sampleRate;
        mSoundWindSize = windSize_ms * mSampleRate * ConfigConst.SOUND_DATA_SIZE/1000;
        mSlideWindSize = strideSize_ms * mSampleRate * ConfigConst.SOUND_DATA_SIZE/1000;
        int arrsize=(int)Math.ceil((float)mSoundWindSize/512)*512;
        //vinhtest mData = new byte[mSoundWindSize + 256 * ConfigConst.SOUND_DATA_SIZE]; //size more than 256 samples for residual data
        mData = new byte[arrsize];
        resetSoundData();
    }

    public void resetSoundData() {
        mcount = 0;
        m1stTime = true;
    }

    public void shiftData(byte[] newdata) {
        if (newdata.length >= mData.length) {//copy the latest data, store in the tail
            System.arraycopy(newdata, newdata.length - mData.length, mData, 0, mData.length);
            mcount = mSoundWindSize;
        } else {// shift new data to mData
            System.arraycopy(mData, newdata.length, mData, 0, mData.length-newdata.length); //shift previous data to the head
            System.arraycopy(newdata, 0, mData, mData.length - newdata.length, newdata.length);
            mcount += newdata.length;
        }
    }

    public byte[] addByteArr(byte[] newdata) {
        byte[] output=null;
        shiftData(newdata);
        if (mcount >= mSoundWindSize) {
            m1stTime = false;
            mcount -= mSlideWindSize;
            byte[] a=new byte[mSoundWindSize];
            output = Arrays.copyOf(mData, mSoundWindSize); //return array
        }
        return output;
    }
    public byte[] getSoundData()
    {
        return mData;
    }

}
