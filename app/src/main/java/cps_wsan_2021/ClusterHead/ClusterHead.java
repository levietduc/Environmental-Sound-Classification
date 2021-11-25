package cps_wsan_2021.ClusterHead;

public class ClusterHead {
    private static final int MAX_ADVERTISE_LIST_ITEM=ClhConst.MAX_ADVERTISE_LIST_ITEM; //max items in waiting list for advertising
    private static final int MAX_PROCESS_LIST_ITEM=ClhConst.MAX_PROCESS_LIST_ITEM; //max items in waiting list for processing
    private boolean mIsSink=false;
    private byte mClhID=ClhConst.DEFAULT_CLUSTER_HEAD_ID;
    private ClhAdvertise mClhAdvertiser;
    private ClhParams mClhParams;

    private ClhScan mClhScanner;

    private ClhProcessData mClhProcessData;
    public ClusterHead(){
        mClhParams=new ClhParams(); //default value
        mClhAdvertiser=new ClhAdvertise(MAX_ADVERTISE_LIST_ITEM);
        mClhProcessData=new ClhProcessData(MAX_PROCESS_LIST_ITEM);
        mClhScanner=new ClhScan(mClhAdvertiser,mClhProcessData);
    }

    //construtor,
    //params: id: cluster head ID: 0..127
    public ClusterHead(ClhParams settings)
    {
        mClhParams=settings;
        mClhAdvertiser=new ClhAdvertise(MAX_ADVERTISE_LIST_ITEM);
        mClhProcessData=new ClhProcessData(MAX_PROCESS_LIST_ITEM);
        mClhScanner=new ClhScan(mClhAdvertiser,mClhProcessData);

    }

    public ClhAdvertise getClhAdvertiser()
    {
        return mClhAdvertiser;
    }
    public ClhScan getClhScanner()
    {
        return mClhScanner;
    }

    // init Cluster Head BLE: include
    // init Advertiser
    // init Scanner
    public int initClhBLE()
    {
        int error;
        error=mClhAdvertiser.initCLHAdvertiser(mClhParams);
        if(error!=ClhErrors.ERROR_CLH_NO) return error;

        mClhScanner.InitClhScan(mClhParams);
        mClhProcessData.initClhProcessData(mClhParams);
        return error;
    }

    public ClhProcessData getClhProcessor(){
        return mClhProcessData;
    }
    public int setClhName(String name)
    {
        int error;

        mClhParams.ClhName=name;
        error=mClhAdvertiser.setAdvname(mClhParams.ClhName);
        if (error!=ClhErrors.ERROR_CLH_NO) return error;
        error=mClhScanner.setScanname(mClhParams.ClhName);
        return error;
    }

     public void clearClhAdvList()
    {
        mClhAdvertiser.clearAdvList();
    }

}
