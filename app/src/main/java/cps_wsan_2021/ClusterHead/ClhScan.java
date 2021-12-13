package cps_wsan_2021.ClusterHead;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ClhScan {
    private BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();
    private BluetoothLeScanner mCLHscanner ;
    private final String LOG_TAG="ClhScanner:";

    private Handler handler = new Handler();
    private Handler handler2 = new Handler();
    private boolean mScanning;
    private byte mClhID=ClhConst.DEFAULT_CLUSTER_HEAD_ID;
    private boolean mIsSink=false;
    private ScanSettings mScanSettings;

    private SparseArray<Long> ClhScanHistoryArray=new SparseArray();

    //private static final int MAX_PROCESS_LIST_ITEM=128;
    //private ClhAdvertisedData clhAdvData=new ClhAdvertisedData();
    private ClhAdvertise mClhAdvertiser;
    private ArrayList<ClhAdvertisedData> mClhProcDataList ;
    private ClhProcessData mClhProcessData;
    private ArrayList<ClhAdvertisedData> mClhAdvDataList;
    private static final int MAX_ADVERTISE_LIST_ITEM=128;
    private ClhParams mClhParams;
    private int mMinClhRssiThreshold;
    private String mClhScanName;
    private Integer mRSSI=-150;
    private  ArrayList<ClhAdvertisedData> printForwardList;
    private ArrayList<Integer> rssiprintForwardList;
    private ArrayList<Integer> rssiProcessData;

    public ClhScan(){}

    public ClhScan(ClhAdvertise clhAdvObj,ClhProcessData clhProcDataObj)
    {//constructor, set 2 alias to Clh advertiser and processor
        setAdvDataObject(clhAdvObj);
        setProcDataObject(clhProcDataObj);
        printForwardList=new ArrayList<ClhAdvertisedData>(64);
        rssiprintForwardList=new ArrayList<>(64);
        rssiProcessData=new ArrayList<>(64);

    }

    public void InitClhScan(ClhParams settings)
    {//constructor, set 2 alias to Clh advertiser and processor
        setClhParams(settings);

    }

    public void setClhParams(ClhParams settings)
    {
        mClhParams=settings;
        mClhID=settings.ClhID;
        mIsSink=settings.isSink;
        mMinClhRssiThreshold=settings.minClhRSSIThreshold;
        mClhScanName=settings.ClhName;
        Log.i(LOG_TAG, mIsSink +" "+mClhID);
    }

    private CountDownTimer mScanTimer1,mScanTimer2,mHistoryScanTimer;

    public int clhScanStart() {
        boolean result=true;
        byte[] advsettings=new byte[16];
        byte[] advData= new byte[256];
        int length;
        final List<ScanFilter> filters = new ArrayList<>();

        if (!mScanning) {
            ClhScanHistoryArray.clear();
                //verify BLE available
            mCLHscanner = mAdapter.getBluetoothLeScanner();
            if (mCLHscanner == null) {
                Log.i(LOG_TAG, "BLE not supported");
                return ClhErrors.ERROR_CLH_BLE_NOT_ENABLE;
            }

            //setting
            ScanSettings ClhScanSettings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                    .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                    .setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
                    .build();

            //set filter: filter name
            ScanFilter filter = new ScanFilter.Builder()
                    .setDeviceName(mClhScanName)
                    .build();
            filters.add(filter);
            Log.i(LOG_TAG, "filters"+ filters.toString());

            mScanSettings =ClhScanSettings;

            // Create 2 timer to preriodically stop scanning (5 minutes), then rest (1 sec) and restart after a pre-defined scan period.
            // to avoid auto disable from Android
            if(mScanTimer1==null) {
                mScanTimer1 = new CountDownTimer(ClhConst.SCAN_PERIOD, ClhConst.SCAN_PERIOD / 2) {
                    @Override
                    public void onTick(long millisUntilFinished) {//tick, not used
                    }

                    @Override
                    public void onFinish() {//timer expire, advertising next packet
                        mScanning = false;
                        mCLHscanner.stopScan(CLHScanCallback);
                        mScanTimer2.start();
                        Log.i(LOG_TAG, "Stop scan");
                    }

                };
            }
            mScanTimer1.start();
            if(mScanTimer2==null) {
                mScanTimer2 = new CountDownTimer(ClhConst.REST_PERIOD, ClhConst.REST_PERIOD / 2) {
                    @Override
                    public void onTick(long millisUntilFinished) {//tick, not used
                    }

                    @Override
                    public void onFinish() {//timer expire, advertising next packet
                        mScanning = true;
                        mCLHscanner.startScan(filters, mScanSettings, CLHScanCallback);
                        mScanTimer1.start();
                    }
                };
            }
            mScanTimer2.cancel();

            /*timer for remove old item in history list, by compare its added time to current time

             */
            if(mHistoryScanTimer==null)
            {
                mHistoryScanTimer = new CountDownTimer(10000, 10000 / 2) {
                    @Override
                    public void onTick(long millisUntilFinished) {//tick, not used
                    }

                    @Override
                    public void onFinish() {//timer expire, advertising next packet
                        Log.i(LOG_TAG, "history timer");

                        if(ClhScanHistoryArray.size()>0)
                        {
                            Log.i(LOG_TAG, "history timer2");

                            for(int i=0;i<ClhScanHistoryArray.size();i++)
                            {

                                long currentTime=System.currentTimeMillis();
                                long period=currentTime-ClhScanHistoryArray.valueAt(i);
                                Log.i(LOG_TAG, "history timer3 "+ClhScanHistoryArray.size()+ " data:"+ClhScanHistoryArray.valueAt(i)+" time:"+currentTime +" period"+ period);

                                if(period>10000)
                                {

                                    ClhScanHistoryArray.removeAt(i);
                                    Log.i(LOG_TAG, "history remove:"+ClhScanHistoryArray.size());

                                }
                                else
                                    break; //items are in ascending of time order, so quit if found an item not expire
                            }
                        }
                        mHistoryScanTimer.start();
                    }
                };
                mHistoryScanTimer.start();

            }



            mScanning = true;
            mCLHscanner.startScan(filters, ClhScanSettings, CLHScanCallback);
            Log.i(LOG_TAG, "Start scan");
        }
        else
        {
            return ClhErrors.ERROR_CLH_SCAN_ALREADY_START;
        }

        return ClhErrors.ERROR_CLH_NO;
    }

    public void stopScanCLH()
    {
        //todo: add stop timer
        if(mScanning) {
            mScanning = false;
            mCLHscanner.stopScan(CLHScanCallback);
            mScanTimer1.cancel();
            mScanTimer2.cancel();
            Log.i(LOG_TAG, "Stop scan");
        }
    }


    private ScanCallback CLHScanCallback = new ScanCallback() {
        @Override
        public final void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            //no need this code since already had name filter
            /*if( result == null
                    || result.getDevice() == null
                    || TextUtils.isEmpty(result.getDevice().getName()) ) {
                Log.i(LOG_TAG, "Empty name space");
                return;
                //if( result == null || result.getDevice() == null)  return;
            }*/

            //check RSSI to remove weak signal ones
            if (result.getRssi()<mMinClhRssiThreshold) {
                Log.i(LOG_TAG,"low RSSI");
                return;
            }
            mRSSI=result.getRssi();

            SparseArray<byte[]> manufacturerData = result.getScanRecord().getManufacturerSpecificData(); //get data
            processScanData(manufacturerData);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e( "BLE", "Discovery onScanFailed: " + errorCode );
            super.onScanFailed(errorCode);
        }
    };


/*process received data of BLE Manufacturer field
 include:
- Manufacturer Specification (in manufacturerData.key): "unique packet ID", include
            2 bytes: 0XAABB: AA: Source Cluster Head ID: 0-127
                            BB: Packet ID: 0-254 (unique for each packet)
 - Manufacturer Data (in manufacturerData.value): remained n data bytes (manufacturerData.size())
-------------------*/

    public void processScanData(SparseArray<byte[]> manufacturerData) {


        if(manufacturerData==null)
        {
            Log.i(LOG_TAG, "no Data");
            return;
        }
        ClhAdvertisedData clhAdvData = new ClhAdvertisedData();
        clhAdvData.parcelAdvData(manufacturerData,0);

        /*TODO: build your filter for received data from here
        in this demo, we filt:
        - reflected packet
        - packet that had been already forwarded (stored in history list)
        -
        */

        if(mClhID==clhAdvData.mSourceID)
        {//reflected data (received cluster head ID = device Clh ID) -> skip

            Log.i(LOG_TAG,"reflected data "+ mClhID + " "+clhAdvData.mSourceID);
            return;
        }
        Log.i(LOG_TAG,"ID data "+ clhAdvData.mSourceID+ "  "+clhAdvData.mPacketID );

        /* check whether packet has been yet recieved by searching the "unique packet ID" history list
         - history list include:
                        Key: unique packet ID
                        life counter: time of the packet lived in history list
          --------------*/

        if (ClhScanHistoryArray.indexOfKey(clhAdvData.mUniquePacketID)<0)
        {//not yet received (not in history list)
            //history not yet full, update new "unique packet ID" to history list, reset life counter
            if(ClhScanHistoryArray.size()<ClhConst.SCAN_HISTORY_LIST_SIZE)
            {
                long time=System.currentTimeMillis();
                ClhScanHistoryArray.append(manufacturerData.keyAt(0),time);
                Log.i(LOG_TAG, "history add:"+ClhScanHistoryArray.size());

            }
            else {
                Log.i(LOG_TAG, "history full:"+ClhScanHistoryArray.size());
                ClhScanHistoryArray.removeAt(ClhScanHistoryArray.size() - 1);
            }

            if(clhAdvData.mDestinationID==127)
            {//broadcast packet, add to both list:processing list and advertising list
                mClhProcessData.addProcessPacketToBuffer(clhAdvData);
                Log.i(LOG_TAG, "Add data to process list, len:" + mClhProcDataList.size());
                if(clhAdvData.mHopCount<5) {
                    mClhAdvertiser.addAdvPacketToBuffer(clhAdvData, false);

                    ArrayList<ClhAdvertisedData> advDataList = mClhAdvertiser.getAdvertiseList();
                    Log.i(LOG_TAG, "Add data to advertised list, len:" + advDataList.size());
                    Log.i(LOG_TAG, "Advertise list at " + (advDataList.size() - 1) + ":"
                            + Arrays.toString(advDataList.get(advDataList.size() - 1).getParcelClhData()));
                }
            }
            else if(clhAdvData.mDestinationID==mClhID)
            {//if this Cluster Head is the destination node, add data to waiting process list
                //TODO: add your code here to process data from the source
                //it can be the routing information or Thingy data or a command
                    mClhProcessData.addProcessPacketToBuffer(clhAdvData);
                    rssiProcessData.add(mRSSI);

            }
            else {//add data to advertising list to forward
                //TODO: add your code here to process data before forwarding
                // in this demo we just check the number of hops
                if(clhAdvData.mHopCount<5) {
                    mClhAdvertiser.addAdvPacketToBuffer(clhAdvData, false);
                    Log.i(LOG_TAG, "Add forwarding data to advertised list");
                    printForwardList.add(clhAdvData);
                    rssiprintForwardList.add(mRSSI);
                }
            }
        }
    }

    //set alias to Clh advertiser
    public void setAdvDataObject(ClhAdvertise clhAdvObj){
        mClhAdvertiser=clhAdvObj;
    }

    //set alias to Clh processor
    public void setProcDataObject(ClhProcessData clhProObj){
        mClhProcessData=clhProObj;
    }

    public ArrayList<ClhAdvertisedData> getprintForwardList()
    {
        return printForwardList;
    }

    public ArrayList<Integer> getrssiForwardList()
    {
        return rssiprintForwardList;
    }

    public ArrayList<Integer> getRssiProcessData()
    {
        return rssiProcessData;
    }

    public int setScanname(String name){
        mClhScanName=name;
        return ClhErrors.ERROR_CLH_NO;
    }

}


