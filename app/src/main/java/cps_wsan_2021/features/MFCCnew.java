package cps_wsan_2021.features;

/**
 * Wav file abstraction layer.
 * Source based on http://www.labbookpages.co.uk/audio/javaWavFiles.html
 *
 *
 */

public class MFCCnew {

    private static int       n_mfcc       		= 13;
    private final static double    fMin                 = 300;
    private final static int       n_fft                = 256;//2048;
    private final static int       hop_length           = 320;
    private final static int	   n_mels               = 32;//128;

    private static double    sampleRate           = 16000;
    private final static double    fMax                 = sampleRate/2.0;

    FFT fft = new FFT();


    public void setSampleRate(int sampleRateVal){
        sampleRate = sampleRateVal;
    }

    public void setN_mfcc(int n_mfccVal) {
        n_mfcc = n_mfccVal;
    }

    public float[] process(double[] doubleInputBuffer) {
        final double[][] mfccResult = dctMfcc(doubleInputBuffer);
        return finalshape(mfccResult);
    }

    //MFCC into 1d
    private float[] finalshape(double[][] mfccSpecTro){
        float[] finalMfcc = new float[mfccSpecTro[0].length * mfccSpecTro.length];
        int k = 0;
        for (int i = 0; i < mfccSpecTro[0].length; i++){
            for (int j = 0; j < mfccSpecTro.length; j++){
                finalMfcc[k] = (float) mfccSpecTro[j][i];
                k = k+1;
            }
        }
        return finalMfcc;
    }

    //DCT to mfcc, librosa
    private double[][] dctMfcc(double[] y){
        final double[][] specTroGram = powerToDb(melSpectrogram(y));
        final double[][] dctBasis = dctFilter(n_mfcc, n_mels);
        double[][] mfccSpecTro = new double[n_mfcc][specTroGram[0].length];
        for (int i = 0; i < n_mfcc; i++){
            for (int j = 0; j < specTroGram[0].length; j++){
                for (int k = 0; k < specTroGram.length; k++){
                    mfccSpecTro[i][j] += dctBasis[i][k]*specTroGram[k][j];
                }
            }
        }
        return mfccSpecTro;
    }


    //mel spectrogram, librosa
    private double[][] melSpectrogram(double[] y){
        //double[][] melBasis = melFilter2(); //PStest
        double[][] melBasis = melFilter();
        double[][] spectro = stftMagSpec(y);
        double[][] melS = new double[melBasis.length][spectro[0].length];
        for (int i = 0; i < melBasis.length; i++){
            for (int j = 0; j < spectro[0].length; j++){
                for (int k = 0; k < melBasis[0].length; k++){
                    melS[i][j] += melBasis[i][k]*spectro[k][j];
                }
            }
        }
        return melS;
    }


    //stft, librosa
    private double[][] stftMagSpec(double[] y){
        //Short-time Fourier transform (STFT)
        final double[] fftwin = getWindow();
        final double[][] frame = yFrame(y);
        double[][] fftmagSpec = new double[1+n_fft/2][frame[0].length];
        double[] fftFrame = new double[n_fft];
        for (int k = 0; k < frame[0].length; k++){
            for (int l =0; l < n_fft; l++){
                fftFrame[l] = fftwin[l]*frame[l][k];
            }
            double[] magSpec = magSpectrogram(fftFrame);
            for (int i =0; i < 1+n_fft/2; i++){
                fftmagSpec[i][k] = magSpec[i];
            }
        }
        return fftmagSpec;
    }

    private double[] magSpectrogram(double[] frame){
        double[] magSpec = new double[frame.length];
        fft.process(frame);
        for (int m = 0; m < frame.length; m++) {
            magSpec[m] = fft.real[m] * fft.real[m] + fft.imag[m] * fft.imag[m];
        }
        return magSpec;
    }


    //get hann window, librosa
    private double[] getWindow(){
        //Return a Hann window for even n_fft.
        //The Hann window is a taper formed by using a raised cosine or sine-squared
        //with ends that touch zero.
        double[] win = new double[n_fft];
        for (int i = 0; i < n_fft; i++){
            win[i] = 0.5 - 0.5 * Math.cos(2.0*Math.PI*i/n_fft);
        }
        return win;
    }

    //frame, librosa
    private double[][] yFrame(double[] ypad){
        final int n_frames = ypad.length / hop_length;
        double[][] winFrames = new double[n_fft][n_frames];
        for (int i = 0; i < n_fft; i++){
            for (int j = 0; j < n_frames; j++){
                winFrames[i][j] = ypad[j*hop_length+i];
            }
        }
        return winFrames;
    }

    //power to db, librosa
    private double[][] powerToDb(double[][] melS){
        //Convert a power spectrogram (amplitude squared) to decibel (dB) units
        //  This computes the scaling ``10 * log10(S / ref)`` in a numerically
        //  stable way.
        double[][] log_spec = new double[melS.length][melS[0].length];
        double maxValue = -100;
        for (int i = 0; i < melS.length; i++){
            for (int j = 0; j < melS[0].length; j++){
                double magnitude = Math.abs(melS[i][j]);
                if (magnitude > 1e-10){
                    log_spec[i][j]=10.0*log10(magnitude);
                }else{
                    log_spec[i][j]=10.0*(-10);
                }
                if (log_spec[i][j] > maxValue){
                    maxValue = log_spec[i][j];
                }
            }
        }

        //set top_db to 80.0
        for (int i = 0; i < melS.length; i++){
            for (int j = 0; j < melS[0].length; j++){
                if (log_spec[i][j] < maxValue - 80.0){
                    log_spec[i][j] = maxValue - 80.0;
                }
            }
        }
        //ref is disabled, maybe later.
        return log_spec;
    }

    //dct, librosa
    private double[][] dctFilter(int n_filters, int n_input){
        //Discrete cosine transform (DCT type-III) basis.
        double[][] basis = new double[n_filters][n_input];
        double[] samples = new double[n_input];
        for (int i = 0; i < n_input; i++){
            samples[i] = (1 + 2*i) * Math.PI/(2.0*(n_input));
        }
        for (int j = 0; j < n_input; j++){
            basis[0][j] = 1.0/Math.sqrt(n_input);
        }
        for (int i = 1; i < n_filters; i++){
            for (int j = 0; j < n_input; j++){
                basis[i][j] = Math.cos(i*samples[j]) * Math.sqrt(2.0/(n_input));
            }
        }
        return basis;
    }


    //mel, librosa
    private double[][] melFilter(){
        //Create a Filterbank matrix to combine FFT bins into Mel-frequency bins.
        // Center freqs of each FFT bin
        final double[] fftFreqs = fftFreq();
        //'Center freqs' of mel bands - uniformly spaced between limits
        final double[] melF = melFreq(n_mels+2);

        double[] fdiff = new double[melF.length-1];
        for (int i = 0; i < melF.length-1; i++){
            fdiff[i] = melF[i+1]-melF[i];
        }

        double[][] ramps = new double[melF.length][fftFreqs.length];
        for (int i = 0; i < melF.length; i++){
            for (int j = 0; j < fftFreqs.length; j++){
                ramps[i][j] = melF[i]-fftFreqs[j];
            }
        }

        double[][] weights = new double[n_mels][1+n_fft/2];
        for (int i = 0; i < n_mels; i++){
            for (int j = 0; j < fftFreqs.length; j++){
                double lowerF = -ramps[i][j] / fdiff[i];
                double upperF = ramps[i+2][j] / fdiff[i+1];
                if (lowerF > upperF && upperF>0){
                    weights[i][j] = upperF;
                }else if (lowerF > upperF && upperF<0){
                    weights[i][j] = 0;
                }else if (lowerF < upperF && lowerF>0){
                    weights[i][j] =lowerF;
                }else if (lowerF < upperF && lowerF<0){
                    weights[i][j] = 0;
                }else {}
            }
        }

        double enorm[] = new double[n_mels];
        for (int i = 0; i < n_mels; i++){
            enorm[i] = 2.0 / (melF[i+2]-melF[i]);
            for (int j = 0; j < fftFreqs.length; j++){
                weights[i][j] *= enorm[i];
            }
        }
        //PStest
        //for(int i=0;i<32;i++)
        //Log.i("test", Arrays.toString(weights[i]));

        return weights;

        //need to check if there's an empty channel somewhere
    }

    //fft frequencies, librosa
    private double[] fftFreq() {
        //Alternative implementation of np.fft.fftfreqs
        double[] freqs = new double[1+n_fft/2];
        for (int i = 0; i <1+ n_fft/2; i++){
            freqs[i] = 0 + (sampleRate/2)/(n_fft/2) * i;
        }
        return freqs;
    }

    //mel frequencies, librosa
    private double[] melFreq(int numMels) {
        //'Center freqs' of mel bands - uniformly spaced between limits
        double[] LowFFreq = new double[1];
        double[] HighFFreq = new double[1];
        LowFFreq[0] = fMin;
        HighFFreq[0] = fMax;
        final double[] melFLow    = freqToMel(LowFFreq);
        final double[] melFHigh   = freqToMel(HighFFreq);
        double[] mels = new double[numMels];
        for (int i = 0; i < numMels; i++) {
            mels[i] = melFLow[0] + (melFHigh[0] - melFLow[0]) / (numMels-1) * i;
        }
        return melToFreq(mels);
    }


    //mel to hz, htk, librosa
    private double[] melToFreqS(double[] mels) {
        double[] freqs = new double[mels.length];
        for (int i = 0; i < mels.length; i++) {
            freqs[i] = 700.0 * (Math.pow(10, mels[i]/2595.0) - 1.0);
        }
        return freqs;
    }


    // hz to mel, htk, librosa
    protected double[] freqToMelS(double[] freqs) {
        double[] mels = new double[freqs.length];
        for (int i = 0; i < freqs.length; i++){
            mels[i] = 2595.0 * log10(1.0 + freqs[i]/700.0);
        }
        return mels;
    }

    //mel to hz, Slaney, librosa
    private double[] melToFreq(double[] mels) {
        // Fill in the linear scale
        final double f_min = 0.0;
        final double f_sp = 200.0 / 3;
        double[] freqs = new double[mels.length];

        // And now the nonlinear scale
        final double min_log_hz = 1000.0;                         // beginning of log region (Hz)
        final double min_log_mel = (min_log_hz - f_min) / f_sp;  // same (Mels)
        final double logstep = Math.log(6.4) / 27.0;

        for (int i = 0; i < mels.length; i++) {
            if (mels[i] < min_log_mel){
                freqs[i] =  f_min + f_sp * mels[i];
            }else{
                freqs[i] = min_log_hz * Math.exp(logstep * (mels[i] - min_log_mel));
            }
        }
        return freqs;
    }


    // hz to mel, Slaney, librosa
    protected double[] freqToMel(double[] freqs) {
        final double f_min = 0.0;
        final double f_sp = 200.0 / 3;
        double[] mels = new double[freqs.length];

        // Fill in the log-scale part

        final double min_log_hz = 1000.0;                         // beginning of log region (Hz)
        final double min_log_mel = (min_log_hz - f_min) / f_sp ;  // # same (Mels)
        final double logstep = Math.log(6.4) / 27.0;              // step size for log region

        for (int i = 0; i < freqs.length; i++) {
            if (freqs[i] < min_log_hz){
                mels[i] = (freqs[i] - f_min) / f_sp;
            }else{
                mels[i] = min_log_mel + Math.log(freqs[i]/min_log_hz) / logstep;
            }
        }
        return mels;
    }



    private double[] melFreq2(int numMels) {
        //'Center freqs' of mel bands - uniformly spaced between limits
        double LowFFreq = fMin;
        double HighFFreq = fMax;

        final double melFLow    = melToFreq2(LowFFreq);
        final double melFHigh   = melToFreq2(HighFFreq);
        double[] mels = new double[numMels];
        double[] freqs=new double[numMels];
        for (int i = 0; i < numMels; i++) {
            mels[i] = melFLow + (melFHigh - melFLow) / (numMels-1) * i;
        }
        for (int i = 0; i < numMels; i++){
            freqs[i]=freqToMel2(mels[i]);
        }
        return freqs;
    }


    public static double melToFreq2(double f) {
        double ret=  1127.0 * Math.log(1 + f / 700.0);
        return ret;
    }

    /**
     * Converting from Mel scale to frequency.
     *
     * @param mel The mel scale values(or a single mel).
     * @returns The frequency values(or a single frequency) in Hz.
     */
    public static double freqToMel2(double mel) {
        double ret= 700.0f * (Math.exp(mel / 1127.0f) - 1.0);
        return ret;
    }


    // log10
    private double log10(double value) {
        return Math.log(value) / Math.log(10);
    }

    private double[][] melFilter2(){
    double[][]filterbanks=new double[n_mels][(1+n_fft/2)];

    //Create a Filterbank matrix to combine FFT bins into Mel-frequency bins.
    // Center freqs of each FFT bin
    final double[] fftFreqs = fftFreq();
    //'Center freqs' of mel bands - uniformly spaced between limits
    final double[] melF = melFreq2(n_mels+2);

    float[] zbuff=new float[1024*4];

    int num_filter=n_mels;
    int coefficients=1+n_fft/2;
    double sampling_freq=sampleRate;
    double low_freq=fMin;
    double high_freq=fMax;



        // The frequency resolution required to put filters at the
        // exact points calculated above should be extracted.
        //  So we should round those frequencies to the closest FFT bin.
        int[] freq_index = new int[n_mels+2];
        for (int ix = 0; ix < n_mels + 2; ix++) {
            freq_index[ix] = (int)((1+n_fft) * melF[ix] / sampleRate);
            //PStest freq_index[ix] = (int)((1+n_fft/2) * melF[ix] / sampleRate);

        }

        for (int i = 0; i < num_filter; i++) {
            int left = freq_index[i];
            int middle = freq_index[i + 1];
            int right = freq_index[i + 2];

            linspace((float)left,(float) right, (right - left + 1), zbuff);
            float[] triA=triangle(zbuff, (right - left + 1), left, middle, right);

            // so... z now contains some values that we need to overwrite in the filterbank
            int index;
            for (int zx = 0; zx < (right - left + 1); zx++) {
                index = ((left + zx) * n_mels) + i;
                filterbanks[index][i] = triA[zx];
            }

        }


        return filterbanks;
    }


    static boolean linspace(float start, float stop, int number, float[] out)
    {
        if (number < 1 || out==null) {
            return false;
        }

        if (number == 1) {
            out[0] = start;
            return true;
        }

        // step size
        float step = (stop - start) / (number - 1);

        // do steps
        for (int ix = 0; ix < number - 1; ix++) {
            out[ix] = start + ix * step;
        }

        // last entry always stop
        out[number - 1] = stop;

        return true;
    }

    /**
     * Triangle, I'm not really sure what this does
     * @param x Linspace output, will be overwritten!
     * @param x_size Size of the linspace output
     * @param left
     * @param middle
     * @param right
     */
    static float[] triangle(float[] x, int x_size, int left, int middle, int right) {
        float[] out=new float[x_size];
        for (int ix = 0; ix < x_size; ix++) {
            if (x[ix] > left && x[ix] <= middle) {
                out[ix] = (x[ix] - left) / (middle - left);
            }

            if (x[ix] < right && middle <= x[ix]) {
                out[ix] = (right - x[ix]) / (right - middle);
            }
        }
        return out;
    }
}
