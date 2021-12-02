package cps_wsan_2021.File;

public class SaveConfig {
    public String mLabel;
    public int mIdx=0;
    public int mDuration=1000; //recording time in ms
    public boolean mCont=true;
    public boolean mIncludeThingyName=true;
    public boolean mIncludeMAC=false;
    public boolean mIncludeTime=true;

    public SaveConfig(String label, int idx, int duration, boolean cont,
                      boolean includeName, boolean includeMAC, boolean includeTime ){
        mLabel=new String(label);
        mIdx=idx;
        mDuration=duration;
        mCont=cont;
        mIncludeThingyName=includeName;
        mIncludeMAC=includeMAC;
        mIncludeTime=includeTime;
    }

    public void setConfig(SaveConfig cfg){
        mLabel=new String(cfg.mLabel);
        mIdx= cfg.mIdx;
        mDuration=cfg.mDuration;
        mCont= cfg.mCont;
        mIncludeThingyName=cfg.mIncludeThingyName;
        mIncludeMAC=cfg.mIncludeMAC;
        mIncludeTime=cfg.mIncludeTime;
    }


}
