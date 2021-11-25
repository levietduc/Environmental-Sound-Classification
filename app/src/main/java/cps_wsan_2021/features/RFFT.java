package cps_wsan_2021.features;

import java.util.Arrays;

public class RFFT {



    /**
     * Compute the one-dimensional discrete Fourier Transform for real input.
     * This function computes the one-dimensional n-point discrete Fourier Transform (DFT) of
     * a real-valued array by means of an efficient algorithm called the Fast Fourier Transform (FFT).
     * @param src Source buffer

     * @returns 0 if OK
     */
    static double[] process(final double[] src,  int n_fft) {
        double[] output=null;
        int n_fft_out_features = (n_fft / 2) + 1;

        double[] fft_input= Arrays.copyOf(src,src.length);
        // declare input and output arrays

return output;
        //int output = software_rfft(fft_input,n_fft, n_fft_out_features);


    }


}
