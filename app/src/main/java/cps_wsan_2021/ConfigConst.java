package cps_wsan_2021;

public class ConfigConst {
    public static final int SOUND_SAMPLING_RATE=16000; //samples/sec
    public static final int SOUND_TIME_SERIES_WINDOWS_SIZE=5000; //ms
    public static final int SOUND_TIME_SERIES_WINDOWS_INC=2500; //ms
    public static final float SOUND_FRAME_LENGTH=0.128f; //sec
    public static final float SOUND_FRAME_STRIDE=0.064f; //sec
    public static final int SOUND_NFFT=2048;
    public static final float SOUND_NOISE_FLOOR=-52; //dB
    public static final boolean ZERO_PAD=false;
    public static final int SOUND_DATA_SIZE=2;

    public static final int SOUND_SPECTROGRAM_MODE=3;

    public final static String[] ylabelStr={"dogbark","babycry","chainsaw", "clocktick",
            "firecrack", "helicopter", "rain", "rooster", "seawave", "sneeze"};


}
