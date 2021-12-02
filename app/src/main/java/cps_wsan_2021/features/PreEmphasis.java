package cps_wsan_2021.features;

import java.util.Arrays;

public class PreEmphasis {
    short[] mData;
    int mShift;
    float mCof;
    float[] mPrev_buffer;
    float[]mEnd_of_signal_buffer;
    int mNext_offset=0;
    boolean mRescale;

    public  PreEmphasis(short[]data, int shift, float cof, boolean rescale){
        mData= Arrays.copyOf(data,data.length);
        mShift=shift;
        mCof=cof;
        mRescale=rescale;
        mPrev_buffer=new float[shift];
        mEnd_of_signal_buffer=new float[shift];
        if (shift < 0) {
            mShift = data.length + shift;
        }
        // we need to get the shift bytes from the end of the buffer...
        System.arraycopy(data,data.length-mShift,mEnd_of_signal_buffer,0,mShift);
    }

    /**
     * Get preemphasized data from the underlying audio buffer...
     * This retrieves data from the signal then preemphasizes it.
     * @param offset Offset in the audio signal (mData)
     * @param length Length of this frame in the audio signal
     */
    float[] get_data(int offset, int length) {
        float out[]=new float[length];
        //for first elements, we use history from previous data tail
        if ((offset) - mShift >= 0) {
            //we use buffer at previous point
            System.arraycopy(mData,offset-mShift,mPrev_buffer,0,mShift);
        }
        // else we'll use the end_of_signal_buffer;

        System.arraycopy(mData,offset,out,0,length);

        // it might be that everything is already normalized here...
        boolean all_between_min_1_and_1 = true;

        // now we have the signal and we can preemphasize
        for (int ix = 0; ix < length; ix++) {
            float now = out[ix];

            // under shift? read from end
            if (offset + ix < mShift) {
                out[ix] = now - (mCof * mEnd_of_signal_buffer[offset + ix]);
            }
            // otherwise read from history buffer
            else {
                out[ix] = now - (mCof * mPrev_buffer[0]);
            }

            if (mRescale && all_between_min_1_and_1) {
                if (out[ix] < -1.0f || out[ix] > 1.0f) {
                    all_between_min_1_and_1 = false;
                }
            }

            // roll through and overwrite last element
            if (mShift != 1) {
                roll(mPrev_buffer, mShift, -1);
            }
            mPrev_buffer[mShift - 1] = now;
        }

        mNext_offset += length;

        // rescale from [-1 .. 1] ?
        if (mRescale && !all_between_min_1_and_1) {
            SoundObj.scale1D(out,1.0f / 32768.0f);
        }

        return out;
    }

    /**
     * Roll array elements along a given axis.
     * Elements that roll beyond the last position are re-introduced at the first.
     * @param input_array
     * @param input_array_size
     * @param shift The number of places by which elements are shifted.
     * @returns EIDSP_OK if OK
     */
    static void roll(float[] input_array, int input_array_size, int shift) {
        if (shift < 0) {
            shift = input_array_size + shift;
        }

        if (shift == 0) {
            return;
        }

        // so we need to allocate a buffer of the size of shift...
        float[] shift_matrix=new float[shift];
        // we copy from the end of the buffer into the shift buffer
        System.arraycopy(input_array,input_array_size-shift,shift_matrix,0,shift);

        // now we do a memmove to shift the array
        System.arraycopy(input_array,0,input_array,shift,input_array_size - shift);

        // and copy the shift buffer back to the beginning of the array
        System.arraycopy(shift_matrix,0,input_array,0,shift);
    }


}
