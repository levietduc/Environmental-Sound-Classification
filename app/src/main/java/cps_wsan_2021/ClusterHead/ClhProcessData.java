package cps_wsan_2021.ClusterHead;

import android.util.Log;

import java.util.ArrayList;

public class ClhProcessData {
    private static final String LOGTAG="Clh_ProcessData";
    public static final int MAX_PROCESS_LIST_ITEM=128;
    private int mMaxProcAllowable=MAX_PROCESS_LIST_ITEM;
    private byte mClhID=ClhConst.DEFAULT_CLUSTER_HEAD_ID;
    private boolean mIsSink=false;
    private  ArrayList<ClhAdvertisedData> mClhProcessDataList;



    public ClhProcessData()
    {
        mClhProcessDataList=new ArrayList<ClhAdvertisedData>(MAX_PROCESS_LIST_ITEM);
        mMaxProcAllowable=MAX_PROCESS_LIST_ITEM;

    }

    public ClhProcessData(int size)
    {
        mClhProcessDataList=new ArrayList<ClhAdvertisedData>(size);
        mMaxProcAllowable=size;
    }

    public void initClhProcessData(ClhParams settings)
    {
        mClhID=settings.ClhID;
        mIsSink=settings.isSink;
    }

    public ArrayList<ClhAdvertisedData> getProcessDataList()
    {
        return mClhProcessDataList;
    }

    public void addProcessPacketToBuffer(ClhAdvertisedData data)
    {
        if(mClhProcessDataList.size()<mMaxProcAllowable) {
            mClhProcessDataList.add(data);
            Log.i(LOGTAG, "Add data to process list, len:" + mClhProcessDataList.size());

        }
    }

    private void outputTotextbox(){
        for(int i=0;i<mClhProcessDataList.size();i++)
        {
            if(i==10) break; //maximum output 10 string at one tick


        }
    }

}
