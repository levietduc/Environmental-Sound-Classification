package cps_wsan_2021.features;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.Arrays;

import cps_wsan_2021.FFT.Complex;
import cps_wsan_2021.FFT.FFT1;

public class SoundObj {
    short[] mSoundData;
    float mSampRate=16000.0f;
    int mNFFT=512;
    float mHopeSize=0.02f;
    float mHopeStride=0.02f;
    float mDbfilt=-54;
    int mFeatureMode=3;

    public SoundObj(byte[]data, float sampl, int nfft, float window,float stride,float filt, int mode)
    {
        ShortBuffer shortbuff= ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN).asShortBuffer();
        mSoundData = new short[shortbuff.limit()];
        shortbuff.get(mSoundData);
        for (int i=1;i<=16000; i++) mSoundData[i-1]=(short)i;
        //mSoundData= Arrays.copyOf(data,data.length);
        mSampRate=sampl;
        mNFFT=nfft;
        mHopeSize=window;
        mHopeStride=stride;
        mDbfilt=filt;
        mFeatureMode=mode;
    }

    public double[][] normalizeInp(double[][] inp)
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
            float sampling_frequency,
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
    double[] power_spectrum(double[] frame, int fft_points)
    {
        double[] magSpec = new double[fft_points/2];
        double[] logmagSpec = new double[fft_points/2];

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


        double temp;
        Complex[] y;
        Complex[] complexSignal = new Complex[fft_points/2];
        double[] absSignal = new double[fft_points / 2];


        for (int i = 0; i < fft_points / 2; i++) {
            //temp = (double) ((data[2 * i] & 0xFF) | (data[2 * i + 1] << 8)) / 32768.0F;
            complexSignal[i] = new Complex(inputfr[i], 0.0);
        }
        y = FFT1.fft(complexSignal); // --> Here I use FFT class
        for (int i = 0; i < (fft_points /2); i++) {
            magSpec[i] = Math.sqrt(Math.pow(y[i].re(), 2) + Math.pow(y[i].im(), 2));
        }

/*
        fftv.process(inputfr);

        for (int m = 0; m < fft_points; m++) {
            magSpec[m] = fftv.real[m] * fftv.real[m] + fftv.imag[m] * fftv.imag[m];
        }*/
        for (int ix = 0; ix < fft_points/2; ix++) {
            //magSpec[ix] = (1.0 / (float)(fft_points)) * (magSpec[ix]*magSpec[ix]);
            magSpec[ix]=(magSpec[ix]*magSpec[ix]);
            if(magSpec[ix]==0) magSpec[ix]=1e-10; //to deal with log(0)
            logmagSpec[ix]=(2.0 / (float)(fft_points))*Math.log10(magSpec[ix]);

        }

        return logmagSpec;
    }

    public double[][] spectrogram()

    {
        double[][] stackdata=stack_frames(mSoundData,mSampRate,
                mHopeSize,mHopeStride,false);

        // normalize data (only when version is above 3)
        double[][] normData =normalizeInp(stackdata);
        int rows=normData.length;
        double[][] spectro= new double[rows][];
        for(int i=0;i<rows;i++) {
            spectro[i] =power_spectrum (normData[i],mNFFT);
        }
        return spectro;
    }





}
