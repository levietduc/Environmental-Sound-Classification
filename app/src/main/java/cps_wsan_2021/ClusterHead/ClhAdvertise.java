package cps_wsan_2021.ClusterHead;

import static cps_wsan_2021.ClusterHead.ClhConst.DEFAULT_CLUSTER_HEAD_ID;
import static cps_wsan_2021.ClusterHead.ClhConst.MAX_ADV_DATA_LENGTH;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.os.CountDownTimer;
import android.os.ParcelUuid;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;


public class ClhAdvertise {

    private final String LOG_TAG = "ClhAdvertising"; //Tag for debug logging via USB

    public final static int ADV_SETTING_BYTE_SENDTXPOWER = 0;
    public final static int ADV_SETTING_SENDTXPOWER_NO = 0;
    public final static int ADV_SETTING_SENDTXPOWER_YES = 1;

    private static String mUUId = null;
    private static ParcelUuid mPUUID;
    private static BluetoothLeAdvertiser mAdvertiser = BluetoothAdapter.getDefaultAdapter().getBluetoothLeAdvertiser();

    private final static int BLE_CLH_ADVERTISING_STATUS_DISABLE=255;
    private final static int BLE_CLH_ADVERTISING_STATUS_STOP=0;
    private final static int BLE_CLH_ADVERTISING_STATUS_START=1;
    private final static int BLE_CLH_ADVERTISING_STATUS_NO_DATA=2;

    private final static long MAX_ADVERTISING_INTERVAL=10000;  //max 10s for an advertising packet interval
    private final static int MAX_ADVERTISE_LIST_ITEM=64; //max queue list for advertising


    private int mMaxAdvAllowable=MAX_ADVERTISE_LIST_ITEM;
    private int mBleClhAdvertisingStatus;
    private boolean mIsSink=false;
    private boolean mTransmitTxPower=false;
    private CountDownTimer mAdvertisingTimer;
    private long mAdvInterval=0;
    private byte[] mAdvsettings=new byte[32];
    private byte mClhID=DEFAULT_CLUSTER_HEAD_ID;
    private byte mCurrentPacketID= (byte) 1;
    private final ArrayList<ClhAdvertisedData >mClhAdvDataList;
    private boolean mIsAdvertising=false;
    private boolean mIsAdvTimerStart=false;
    private ClhParams mClhParams;
    private boolean mIsAdvHalt=false;
    private byte mSinkID=0;

    private ArrayList<ClhAdvertisedData > printSendList=new ArrayList<ClhAdvertisedData>(64);;


    public ClhAdvertise(){//constructor with no params
        mClhAdvDataList= new ArrayList<ClhAdvertisedData>(MAX_ADVERTISE_LIST_ITEM);
        mMaxAdvAllowable=MAX_ADVERTISE_LIST_ITEM;

    }

    //constructor with param
    public ClhAdvertise(int size){
        mClhAdvDataList= new ArrayList<ClhAdvertisedData>(size);
        mMaxAdvAllowable=size;
    }



    public int initCLHAdvertiser(ClhParams settings)
    {
        int error;
       // byte[] advsettings=new byte[16];
        Log.i(LOG_TAG, "Start Intialize func");
        if ((error=checkBLEAdvertiser())!=ClhErrors.ERROR_CLH_NO)
            return error;
        setClhParams(settings);
        //mClhID=settings.ClhID;
        //mIsSink=settings.isSink;
        //mTransmitTxPower=settings.TransmitTxPower;
        //mAdvInterval=settings.advertisingInterval;

        if (mUUId == null) {
            //set  random UUID to this advertising obj
            mUUId = UUID.randomUUID().toString().toUpperCase();
            mPUUID = new ParcelUuid(UUID.fromString(mUUId));
        }

        //start default advertising: name, no TX power
        //advsettings[ADV_SETTING_BYTE_SENDTXPOWER] = ADV_SETTING_SENDTXPOWER_NO;

        //on advertising -> stop advertising
        stopAdvertiser();
        //set default Bluetooth name
        if (!BluetoothAdapter.getDefaultAdapter().setName(ClhConst.clusterHeadName)) {
            Log.i(LOG_TAG, "Advertiser: set name fail" );
            return ClhErrors.ERROR_CLH_BLE_SETNAME_FAIL;
        }

        //set up timer for each packet advertising, expire interval in mAdvInterval
        mAdvertisingTimer=new CountDownTimer(mAdvInterval,100) {
            @Override
            public void onTick(long millisUntilFinished) {//tick, not used
            }

            @Override
            public void onFinish() {//timer expire, advertising next packet
                if(mIsAdvertising){
                    nextAdvertisingPacket(); //advertise next packet
                    Log.i(LOG_TAG,"Done CountDownTimer");
                }
            }
        };
        mAdvertisingTimer.cancel();

        Log.i(LOG_TAG,"End Initializing func");

        return ClhErrors.ERROR_CLH_NO;
    }



    //------------------------
    // advertising next data in the waiting list
    public void nextAdvertisingPacket(){

        if (mClhAdvDataList.size()>0)
        {//list not empty, advertise item 0 in the list
            byte[] mAdvData = mClhAdvDataList.get(0).getParcelClhData();
            Log.i(LOG_TAG,"new data: size:"+mClhAdvDataList.size() + ",data:" +Arrays.toString(mAdvData));
            //printSendList.add(mClhAdvDataList.get(0));
            mClhAdvDataList.remove(0);
            stopAdvertiser();
            startAdvertiser(mAdvsettings,mAdvData);
        }
        else
        {//empty list
            Log.i(LOG_TAG,"Halt advertsing");
            mBleClhAdvertisingStatus=BLE_CLH_ADVERTISING_STATUS_NO_DATA;
            if(mIsAdvTimerStart) {
                mAdvertisingTimer.cancel();//stop timer
                mIsAdvTimerStart = false;
            }
            mIsAdvHalt=true;
            stopAdvertiser();
        }
    }



    /*==========================
    Add a packet to queuing buffer for advertising
    @params:
    data: data to be advertising, start advertiser if not yet started
    isOrginal: =true: packet is from internal process of this cluster head.
            =false: packet received from other cluster head, need forwarding
     */
    public void addAdvPacketToBuffer(ClhAdvertisedData data,boolean isOrginal)
    {
        if(mClhAdvDataList.size()<mMaxAdvAllowable) {
            if(isOrginal) {//this packet come from this device-> increase PacketID
                mCurrentPacketID++;
                data.setPacketID(mCurrentPacketID);
            }
            else
            {//received packet over BLE scan, from other cluster head -> increase hopscount
                byte hopcounts=data.getHopCounts();
                hopcounts++;
                data.setHopCount(hopcounts);
            }
            ClhAdvertisedData adata=new ClhAdvertisedData(data);
            mClhAdvDataList.add(adata);
            Log.i(LOG_TAG,"add Adv packet to buffer, size:"+mClhAdvDataList.size());
            Log.i(LOG_TAG, "Advertise list at " + (mClhAdvDataList.size() - 1) + ":"
                    + Arrays.toString(mClhAdvDataList.get(mClhAdvDataList.size() - 1).getParcelClhData()));

            if(mIsAdvHalt) {
                mIsAdvHalt=false;
                nextAdvertisingPacket(); //start advertising if is stopping
            }

        }
    }


    //----------------------------------------------------
    // parcel Sound data and add to waiting list for advertising
    private static int mSoundcount=0;
    public int addAdvSoundData(byte[]data)
    {
        if(mIsSink) return 0; //not found Sink yet, or this device is Sink
        if((data!=null) && data.length>0) {
            if(mSoundcount++==0)
            {
                ClhAdvertisedData advData = new ClhAdvertisedData();
                advData.setSourceID(mClhID);
                advData.setDestId((byte)0 ); //sink ID is 0
                advData.setNextHopId((byte) 127); //Todo, not used yet
                advData.setHopCount((byte) 1);
                advData.setUserData(data);

                Log.i(LOG_TAG,"add new sound data:"+ Arrays.toString(data));
                addAdvPacketToBuffer(advData,true);
                printSendList.add(advData);
                mSoundcount=0;
            }
            else return 2;
        }
        else return 0;
        return 1;
    }


    public void setClhParams(ClhParams in)
    {
        Log.i(LOG_TAG,"setClhParams: func");
        mClhParams=in;
        mIsSink=in.isSink;
        mClhID=in.ClhID;
        mTransmitTxPower=in.TransmitTxPower;
        mAdvInterval=in.advertisingInterval;
        Log.i(LOG_TAG, mIsSink +" "+mClhID);
    }

    /*----------
 Start advertiser
 @param
  data[]: data

 --------*/
    private int startAdvertiser(byte[] settings, byte[] data) {
        //setting and start advertiser
        //@param: settings: configuration
        //data: input data [0]: length
        //  if length =0 or too long (total lenght of data + name +txpower(option) >MAX_ADV_DATA_LENGTH) return error

        int txpower;
        Log.i(LOG_TAG,"Start startAdvertizer func");

        if (data==null)
        {
            return ClhErrors.ERROR_CLH_ADV_NO_DATA;
        }


        /*if(!mIsAdvTimerStart)
        {
            mIsAdvTimerStart=true;
            mAdvertisingTimer.start();
            Log.i(LOG_TAG,"start CountDownTimer in startAdv");

        }*/

        switch (mClhParams.TxPower)
        {
            case ClhParams.CLH_TX_POWER_ULTRA_LOW:
                txpower=AdvertiseSettings.ADVERTISE_TX_POWER_ULTRA_LOW;
                break;
            case ClhParams.CLH_TX_POWER_LOW:
                txpower=AdvertiseSettings.ADVERTISE_TX_POWER_LOW;
                break;
            case ClhParams.CLH_TX_POWER_MEDIUM:
                txpower=AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM;
                break;
            case ClhParams.CLH_TX_POWER_HIGH:
            default:
                txpower=AdvertiseSettings.ADVERTISE_TX_POWER_HIGH;
                break;
        }

        AdvertiseSettings.Builder advSettingsBuilder = new AdvertiseSettings.Builder()
                .setTxPowerLevel(txpower)
                .setConnectable(false)
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY);
        AdvertiseSettings advSettings = advSettingsBuilder.build();

        //enable/disable send device name and txpower
        int advDatalen=3; //count the length of advertise data
        AdvertiseData.Builder advDataBuilder = new AdvertiseData.Builder();
        advDataBuilder.setIncludeDeviceName(true); //send name, used for filtering ClusterHead to other BLE device in scanner
        advDatalen+=BluetoothAdapter.getDefaultAdapter().getName().length()+2; //count advertising data's length
        if(settings==null) advDataBuilder.setIncludeTxPowerLevel(false);
        else {
            if (mTransmitTxPower) {
                advDataBuilder.setIncludeTxPowerLevel(true);
                advDatalen += 3;    //add Txpower data length
            }
            else advDataBuilder.setIncludeTxPowerLevel(false);
        }
        // total length include: 3(default) + 2 (header of BLE MANU DATA) + data + name (varied) + txpower (3:option)
        advDatalen=3+ 2 + data.length+advDatalen ;
        Log.i(LOG_TAG, "current length: "+ advDatalen);
        if(advDatalen>MAX_ADV_DATA_LENGTH)
        {//if data length too long
            Log.i(LOG_TAG, "Too long advertising data:" + advDatalen);
            return ClhErrors.ERROR_CLH_ADV_TOO_LONG_DATA;
        }
        else
        {
            int len=data.length;
            int manuSpec=(((int)data[0]<<8)&0x7F00)+ ((int)(data[1])&0x00FF);
            byte[] advData= Arrays.copyOfRange(data,2, len);
            advDataBuilder.addManufacturerData(manuSpec,advData);
            Log.i(LOG_TAG, "send manufature data, total length:" +advDatalen);
            Log.i(LOG_TAG, "send data length:" +len);
            Log.i(LOG_TAG, "Manu Spec: 0x" + data[0] + ","+data[1]);
            Log.i(LOG_TAG, "Manu Data: "+ Arrays.toString(advData));
        }
        mIsAdvertising=true;
        AdvertiseData sendData = advDataBuilder.build();
        mAdvertiser.startAdvertising(advSettings, sendData, null, advertisingCallback);
        Log.i(LOG_TAG,"End Start advertizer func");
        return ClhErrors.ERROR_CLH_NO;
    }

    public void stopClhAdvertiser()
    {
        if(mAdvertisingTimer!=null) mAdvertisingTimer.cancel();//stop timer
        mIsAdvTimerStart=false;
        mIsAdvHalt=false;
        stopAdvertiser();
        Log.i(LOG_TAG, "stopClhAdvertiser func");

    }

    private void stopAdvertiser()
    {
        if(mIsAdvertising==false)
            return;
        mIsAdvertising=false;
        mAdvertiser.stopAdvertising(advertisingCallback);
        Log.i(LOG_TAG, "stopAdvertiser func");
    }

    private final AdvertiseCallback advertisingCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            Log.i(LOG_TAG, "Start Advertising Success "+ settingsInEffect.describeContents());
            mBleClhAdvertisingStatus=BLE_CLH_ADVERTISING_STATUS_START;
            mAdvertisingTimer.start();//start timer for next packet
            Log.i(LOG_TAG,"Start CountDownTimer in AdvertiseCallback");

        }

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            Log.i(LOG_TAG, "Advertising onStartFailure: " + errorCode);
            stopAdvertiser();
        }
    };

    public void clearAdvList()
    {
        mClhAdvDataList.clear();
    }
    public ArrayList<ClhAdvertisedData> getAdvertiseList()
    {
        return mClhAdvDataList;
    }
    private int checkBLEAdvertiser()
    {
        //verify BLE available
        if (!BluetoothAdapter.getDefaultAdapter().isMultipleAdvertisementSupported()) {
            Log.i(LOG_TAG, "Multiple advertisement not supported");
            return ClhErrors.ERROR_CLH_ADV_MULTI_ADVERTISER;
        }
        if ((mAdvertiser=BluetoothAdapter.getDefaultAdapter().getBluetoothLeAdvertiser()) == null) {
            Log.i(LOG_TAG, "BLE not supported");
            return ClhErrors.ERROR_CLH_BLE_NOT_ENABLE;
        }
        return ClhErrors.ERROR_CLH_NO;
    }

    public ArrayList<ClhAdvertisedData> getprintSendList()
    {
        return printSendList;
    }

    public int setAdvname(String name){
        if (!BluetoothAdapter.getDefaultAdapter().setName(name)) {
            Log.i(LOG_TAG, "Advertiser: set name fail" );
            return ClhErrors.ERROR_CLH_BLE_SETNAME_FAIL;
        }
        Log.i(LOG_TAG, "Set Name:" +BluetoothAdapter.getDefaultAdapter().getName());
        return ClhErrors.ERROR_CLH_NO;
    }

}
