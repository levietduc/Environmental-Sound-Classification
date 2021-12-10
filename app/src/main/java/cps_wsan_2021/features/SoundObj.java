package cps_wsan_2021.features;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.Arrays;

public class SoundObj {
    final static float PRESET_LOW_FREQ = 300.0f;
    final static float PRESET_POWER_LOWFILT = -52.0f;

    SoundData mSoundData;
    short[] mDataToProc;
    byte[] mWindDataByte;
    int mSampRate=16000;
    int mNFFT=512;
    float mFrameLength =0.02f;
    float mHopStride=0.02f;
    float mDbfilt=PRESET_POWER_LOWFILT;
    int mFeatureMode=3;
    boolean mZeroPad=false;
    float mLowFreq=300;
    float mHighFreq=8000;
    float mPreEmphCoff=0.98f;
    int mPreEmphShift=1;
    float[] mPreBuff=new float[64];
    float[] mEndOfSignBuff=new float[64];
    int mNextOffset=0;

    /*init memory and windows to store streamed audio data
    //contScanWindSize: size of windows to be extracted for features
    //contScanWindStride: sliding stride*/
    public SoundObj(int samplingRate, int timeSeriesWindSize, int timeSeriesWindInc,
                    int nFFT, float frameLength,float frameStride,
                    float filtdB, boolean zeropad,
                    int mode)
    {//for spectrogram mode

        mSoundData=new SoundData((int)samplingRate,timeSeriesWindSize,timeSeriesWindInc);
        mSampRate=samplingRate;
        mHighFreq=mSampRate/2.0f;
        mNFFT=nFFT;
        mFrameLength =frameLength;
        mHopStride=frameStride;
        mDbfilt=filtdB;
        mZeroPad=zeropad;
        mFeatureMode=mode;

    }
/*
    public SoundObj(byte[]data, float sampl, int nfft, float windowLen,float stride,float filt,
                    float lowfreq, float highfreq,int mode)
    {//for MFE mode
        ShortBuffer shortbuff= ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
        mSoundData = new short[shortbuff.limit()];
        shortbuff.get(mSoundData);
        mSampRate=sampl;
        if(highfreq>0)
            mHighFreq=highfreq;
        else
            mHighFreq=sampl/2;

        if(lowfreq<=0)
            mLowFreq=PRESET_LOW_FREQ;
        else
            mLowFreq=lowfreq;
        mNFFT=nfft;
        mHopeSize=windowLen;
        mHopeStride=stride;
        mDbfilt=filt;
        mFeatureMode=mode;

        if (mPreEmphShift < 0) {
            mPreEmphShift = data.length + mPreEmphShift;
        }
        // we need to get the shift bytes from the end of the buffer...
        System.arraycopy(data,data.length-mPreEmphShift,mEndOfSignBuff,0,mPreEmphShift);


    }




    public SoundObj(byte[]data, float sampl, int nfft, float windowLen,float stride,
                    float lowfreq, float highfreq,float preEmphCoff, int preEmpShift,
                    int mode)
    {//for MFCC mode
        ShortBuffer shortbuff= ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
        mSoundData = new short[shortbuff.limit()];
        shortbuff.get(mSoundData);
        mSampRate=sampl;
        if(highfreq>0)
            mHighFreq=highfreq;
        else
            mHighFreq=sampl/2;

        if(lowfreq<=0)
            mLowFreq=PRESET_LOW_FREQ;
        else
            mLowFreq=lowfreq;
        mNFFT=nfft;
        mHopeSize=windowLen;
        mHopeStride=stride;
        mFeatureMode=mode;
        mPreEmphCoff=preEmphCoff;
        mPreEmphShift=preEmpShift;
    }*/

    public Float[] getSpectrogram(byte[]data)
    {
        boolean res;
        Float[] flatsp=null;
        byte[] calArr;
        calArr=mSoundData.addByteArr(data);//add new data to buffer
        if(calArr!=null)
        {//have enough data to proceed

            ShortBuffer shortbuff= ByteBuffer.wrap(calArr).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
            mDataToProc = new short[shortbuff.limit()];
            shortbuff.get(mDataToProc);
            double[][] pwrspec=spectrogram(mDataToProc); //Todo
            int rows=pwrspec.length;
            int cols=pwrspec[0].length;
            flatsp=new Float[rows*cols];
            int pos=0;
            for(int i=0;i<rows;i++)
            {
                for(int j=0;j<cols;j++) {
                    pos = i * cols + j;
                    flatsp[pos] = (Float)(float)pwrspec[i][j];
                }
            }
        }
        return flatsp;
    }

    public void resetSoundBuff(){
        mSoundData.resetSoundData();
    }

    private double[][] normalizeInp(double[][] inp)
    {
        double[][] retArr=null;
        if(inp==null) return retArr;

        int rows= inp.length;
        int cols=inp[0].length;
        boolean all_between_min_1_and_1 = true;

        if (mFeatureMode >= 3) {
            // it might be that everything is already normalized here...
            for (int ix = 0; ix < rows; ix++) {
                for (int jx=0; jx<cols;jx++) {
                    if (inp[ix][jx] < -1.0f || inp[ix][jx] > 1.0f) {
                        all_between_min_1_and_1 = false;
                        jx=cols;
                        ix=rows;
                    }
                }
            }
        }

        float scalef=1;
        if (!all_between_min_1_and_1) {
            scalef=1/32768.0f;
        }
        retArr=new double[rows][cols];
        for (int ix = 0; ix < rows; ix++) {
            for (int jx=0; jx<cols;jx++) {
                retArr[ix][jx] =inp[ix][jx]*scalef;
            }
        }

        return retArr;
    }


    /**
     * Frame a signal into overlapping frames.
     * @param sampling_frequency (int): The sampling frequency of the signal.
     * @param frame_length (float): The length of the frame in second.
     * @param stride (float): The stride between frames.
     * @param zero_padding (bool): If the samples is not a multiple of
     *        frame_length(number of frames sample), zero padding will
     *        be done for generating last frame.
     * @returns EIDSP_OK if OK
     */
    private double[][] stack_frames(
            short[] data,
            int sampling_frequency,
            float frame_length,
            float stride,
            boolean zero_padding)
    {

        int length_signal = data.length;
        int frame_sample_length;
        int total_length=0;
        float frame_stride;
        int length;
        if (mFeatureMode == 1) { //raw data only
            frame_sample_length = Math.round(sampling_frequency * frame_length);
            frame_stride = Math.round(sampling_frequency* stride);
            length = frame_sample_length;
        }
        else {
            frame_sample_length = (int) Math.ceil(sampling_frequency * frame_length);
            frame_stride = (float) Math.ceil(sampling_frequency* stride);
            length = (frame_sample_length - (int)frame_stride);
        }

         int numframes;
         int len_sig;

        if (zero_padding) {
            // Calculation of number of frames
            numframes = (int)(Math.ceil((length_signal - length) / frame_stride));

            // Zero padding
            len_sig = (int)((float)numframes * frame_stride) + frame_sample_length;

            total_length = len_sig;
        }
        else {
            numframes = (int)Math.floor((float)(length_signal - length) / frame_stride);
            len_sig = (int)((float)(numframes - 1) * frame_stride + frame_sample_length);

            total_length = len_sig;;
        }

        double[][] outp=new double[numframes][frame_sample_length];

        for(int fr=0;fr<numframes;fr++)
        {
            for(int j=0;j<frame_sample_length;j++)
            {
                int ix = fr * (int) frame_stride + j;
                if(ix< data.length) {
                    outp[fr][j] = (double) data[ix];
                }
                else
                {
                    outp[fr][j]=0;
                }
            }
        }
        return  outp;

    }


    /**
     * Power spectrum of a frame
     * @param frame Row of a frame
     * @param fft_points (int): The length of FFT. If fft_length is greater than frame_len, the frames will be zero-padded.
     * @returns power_spectrum 1/N*mag(FFT)
     */
    private double[] power_spectrum(double[] frame, int fft_points)
    {
        double[] magSpec = new double[fft_points];
        double[] halfmagSpec=new double[fft_points/2+1];
        double[] logmagSpec = new double[fft_points/2+1];

        FFT fftv=new FFT();
        double[] inputfr=new double [fft_points];
        // truncate if needed
        if (frame.length >= fft_points) {
            inputfr=Arrays.copyOf(frame,fft_points);
        }
        else
        {
            Arrays.fill(inputfr,0.0f);
            inputfr=Arrays.copyOf(frame,frame.length);
        }

        fftv.process(inputfr);

        for (int m = 0; m < fft_points; m++) {
            magSpec[m] = fftv.real[m] * fftv.real[m] + fftv.imag[m] * fftv.imag[m];
            magSpec[m]/=fft_points;
        }

        //halfFFT[0]=magSpec[0];
        //halfFFT[fft_points/2-1]=magSpec[fft_points/2-1]
        for (int i=0;i<fft_points/2+1;i++)
        {
            halfmagSpec[i]=magSpec[i];
        }
        float noise_floor_db=-52.0f;
        float noise=noise_floor_db*-1.0f;
        float noise_scale=1.0f / ((noise_floor_db * -1.0f) + 12.0f);

        for (int ix = 0; ix < fft_points/2+1; ix++) {
            //magSpec[ix] = (1.0 / (float)(fft_points)) * (magSpec[ix]*magSpec[ix]);
            //magSpec[ix]=(magSpec[ix]*magSpec[ix]);
            if(halfmagSpec[ix]==0) halfmagSpec[ix]=1e-10; //to deal with log(0)
            logmagSpec[ix]=10.0*Math.log10(halfmagSpec[ix]);

            logmagSpec[ix]=(logmagSpec[ix]+noise)*noise_scale;
            if(logmagSpec[ix]<0)
            {
                logmagSpec[ix]=0;
            }
        }

        return logmagSpec;
    }

    private double[][] spectrogram(short[] data)
    {
        //divide data window into frames for power spectrum calculation
        double[][] stackdata=stack_frames(data,mSampRate,
                mFrameLength,mHopStride,false);

        // normalize data (only when version is above 3)
        double[][] normData =normalizeInp(stackdata);

        int rows=normData.length;
        double[][] spectro= new double[rows][];
        //calculate power spectrum for each frame
        for(int i=0;i<rows;i++) {
            spectro[i] =power_spectrum (normData[i],mNFFT);
        }
        return spectro;
    }

    public byte[] getSoundData()
    {
        return mSoundData.getSoundData();
    }

/*
    public float[] mfe()
    {
        float[] preEmData;
        PreEmphasis preEmp=new PreEmphasis(mSoundData,mPreEmphShift,mPreEmphCoff,true);
        preEmData=preEmp.get_data(0,mSoundData.length);


    }
*/

    public static void scale1D(float[]data, float scale)
    {
        if (scale == 1.0f) return;

        for (int ix = 0; ix < data.length; ix++) {
            data[ix] =data[ix]*scale;
        }
    }
    public static void scale2D(float[][]data, float scale)
    {
        if (scale == 1.0f) return;
        for (int ix = 0; ix < data.length; ix++) {
            for (int jx=0; jx<data[0].length;jx++) {
                data[ix][jx] =data[ix][jx]*scale;
            }
        }
    }

    public short[] byteToShortArr(byte[] in, ByteOrder order)
    {//boolean=0:
        ShortBuffer shortbuff= ByteBuffer.wrap(in).order(order).asShortBuffer();
        short[] retData = new short[shortbuff.limit()];
        shortbuff.get(retData);
        return retData;
    }

}
