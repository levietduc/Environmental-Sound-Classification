package cps_wsan_2021;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

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

    public final static List<String> colorOrder=new ArrayList<String>(){{
        add("yellow");
        add("orange");
        add("red");
        add("blue");
        add("green");
        add("purple");
        add("cyan");
        add("darkYellow");
        add("darkOrange");
        add("darkRed");
        add("darkGreen");
        add("darkPurple");
        add("darkCyan");
        add("darkBlue");
    }};

    public final static LinkedHashMap<String, Integer> colorDefine = new LinkedHashMap<String, Integer>() {{
        put("yellow",0x00FF8000);
        put("orange",0x00FF2000);
        put("red",0x00FF0000);
        put("green",0x0000FF00);
        put("blue",0x000000FF);
        put("purple",0x00FF0030);
        put("cyan",0x0000FF80);
        put("darkYellow",0x00502000);
        put("darkOrange",0x00601000);
        put("darkRed",0x00200000);
        put("darkGreen",0x00002000);
        put("darkBlue",0x00000020);
        put("darkPurple",0x00400010);
        put("darkCyan",0x00004020);

    }};

}
