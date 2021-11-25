package cps_wsan_2021;

import static cps_wsan_2021.common.Utils.REQUEST_ACCESS_COARSE_LOCATION;
import static cps_wsan_2021.common.Utils.REQUEST_ACCESS_FINE_LOCATION;
import static cps_wsan_2021.common.Utils.checkIfVersionIsMarshmallowOrAbove;
import static cps_wsan_2021.common.Utils.checkIfVersionIsQ;
import static cps_wsan_2021.common.Utils.showToast;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.ParcelUuid;
import android.provider.Settings;
import android.text.InputFilter;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.cps_wsan_2021_scratch.R;
import com.example.cps_wsan_2021_scratch.ml.SoundClassification;
import com.google.android.material.textfield.TextInputEditText;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.model.Model;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import cps_wsan_2021.ClusterHead.ClhAdvertise;
import cps_wsan_2021.ClusterHead.ClhAdvertisedData;
import cps_wsan_2021.ClusterHead.ClhConst;
import cps_wsan_2021.ClusterHead.ClhParams;
import cps_wsan_2021.ClusterHead.ClhProcessData;
import cps_wsan_2021.ClusterHead.ClhScan;
import cps_wsan_2021.ClusterHead.ClusterHead;
import cps_wsan_2021.FFT.Complex;
import cps_wsan_2021.FFT.FFT1;
import cps_wsan_2021.common.ExtendedBluetoothDevice;
import cps_wsan_2021.common.InputFilterMinMax;
import cps_wsan_2021.common.MessageDialogFragment;
import cps_wsan_2021.common.PCMtoWAV;
import cps_wsan_2021.common.PermissionRationaleDialogFragment;
import cps_wsan_2021.common.Utils;
import cps_wsan_2021.features.MFCCnew;
import cps_wsan_2021.thingy.ThingyService;
import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanFilter;
import no.nordicsemi.android.support.v18.scanner.ScanResult;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;
import no.nordicsemi.android.thingylib.ThingyListener;
import no.nordicsemi.android.thingylib.ThingyListenerHelper;
import no.nordicsemi.android.thingylib.ThingySdkManager;
import no.nordicsemi.android.thingylib.utils.ThingyUtils;


/** The request code for requesting [Manifest.permission.READ_EXTERNAL_STORAGE] permission. */

public class MainActivity extends AppCompatActivity implements  PermissionRationaleDialogFragment.PermissionDialogListener,
        ThingySdkManager.ServiceConnectionListener
{

    private static final String LOGTAG = "ClhMain" ;
    private static final int MAX_CONNECTED_THINGIES=3; //max thingies can be connected in a clusterhead
    private static final int SCAN_DURATION = 10000;
    private final static long THINGY_SCAN_DURATION = 5000; //scan Thingy for 5 sec
    private final static long THINGY_BATCH_SCAN_DURATION=500;
    private final static int MAX_THINGIES_CLUSTER=5; //max thingies a cluster head can connect to
    private final static int THINGY_MICROPHONE_SAMPLING_FREQ=16000;
    private ThingySdkManager mThingySdkManager;
    private ThingyService.ThingyBinder mBinder;
    private ArrayList<ExtendedBluetoothDevice> mConnectedBleDeviceList;
    private static boolean mAutoConnect=false;
    private static boolean mAutoDisConnect=false;
    private Handler mThingyScannerHandler;
    private int mThingyListIndex;
    private int mThingyConnected;
    private BluetoothDevice mDevice;
    private ExtendedBluetoothDevice mExtDevice;
    private boolean mIsScanningThingy=false;
    final BluetoothLeScannerCompat mThingyScanner = BluetoothLeScannerCompat.getScanner();
    private boolean mRetryConnecting=false;
    private boolean mWaitingMtuResponse=false;

    private final List<ExtendedBluetoothDevice> mScanresults=new ArrayList<>();
    private List<ExtendedBluetoothDevice> mSaveScan;
    private List<ExtendedBluetoothDevice> mAvaibleThingies =new ArrayList<>();

    private Button mScanClusterButton;
    private Button mConnectThingyButton;
    private Button mDisconnectThingButton;
    private Button mSaveSoundButton;

    private TextView mClusterLogText;
    private TextView mSoundEventLog;
    private EditText mtxtClhNameInp;




    private Spinner mTxPowerSelect;
    private Button mStartClh;
    private Button mClearLog;
    private TextView mClhLogText;
    private CheckBox mIsSinkCheckBox;
    private TextInputEditText mClhIDTextBox;
    private Byte mTxPower;
    //private ClhAdvertisedData mClhData=new ClhAdvertisedData();
    private boolean mIsSink=false;
    private ClhParams mClhParams;
    private byte mSinkID=0;
    private byte mClhID=2;
    private byte mClhDestID=0;
    private byte mClhHops=0;
    private byte mClhThingyID=1;
    private byte mClhThingyType=1;
    private int mClhThingySoundPower=100;
    ClusterHead mClh;
    ClhAdvertise mClhAdvertiser;
    ClhScan mClhScanner;
    ClhProcessData mClhProcessor;
    private boolean mIsAdvertising=false;
    private CountDownTimer mReconnThingyTimer;

    private File pathSoundData;
    //private File[] soundFileName=new File[10];
    private LinkedHashMap<String, File> soundFileName=new LinkedHashMap<>();
    private LinkedHashMap<String, File> soundFileNameWav=new LinkedHashMap<>();

    private final int NUM_OF_SOUND_PACKET=64; //16000/256
    private byte[][] mSoundArr=new byte[5][512*NUM_OF_SOUND_PACKET];
    private LinkedHashMap<String, Integer> mSoundVarCount=new LinkedHashMap<>();
    private LinkedHashMap<String, byte[]> mSoundVar=new LinkedHashMap<>();



    private final static LinkedHashMap<String, Integer> colorDefine = new LinkedHashMap<String, Integer>() {{
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

    private boolean mSaveEnable=false;
    private ClhParams getClusterHeadSettings()
    {

        mIsSink=mIsSinkCheckBox.isChecked();
        Log.i(LOGTAG, "isSink:"+mIsSink);
        int num= Integer.valueOf(mClhIDTextBox.getText().toString());
        mClhID=(byte)(num);
        Log.i(LOGTAG, "ClhID:"+mClhID);
        mTxPower=(byte)mTxPowerSelect.getSelectedItemPosition();
        Log.i(LOGTAG, "TxPower:"+mTxPower);

        ClhParams clhsettings=new ClhParams();
        clhsettings.ClhID=mClhID;
        clhsettings.isSink=mIsSink;
        clhsettings.TxPower=mTxPower;
        //set cluster head name
        String txtname;
        if (mtxtClhNameInp.getText().toString().length()==0)
        {
            txtname= ClhConst.clusterHeadName;
        }
        else {
            String txtStr = mtxtClhNameInp.getText().toString();
            if (txtStr.length()>3)
            {
                txtname=txtStr.substring(txtStr.length()-4,txtStr.length()-1);
            }
            else
            {
                txtname=txtStr;
            }


        }
        clhsettings.ClhName=txtname;

        return clhsettings;
    }

    /* start scanning for avaible thingies
    result: List<ExtendedBluetoothDevice> mScanresults
     */
    public void startScanThingies() {
        if(mIsScanningThingy) return; //is scanning, quit

        Log.i(LOGTAG, "startScan Thingies");

        final ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setReportDelay(THINGY_BATCH_SCAN_DURATION)
                .setUseHardwareBatchingIfSupported(false) //PStest
                .setUseHardwareFilteringIfSupported(true)
                .build();
        final List<ScanFilter> filters = new ArrayList<>();
        //filter result with Thingy UUID
        filters.add(new ScanFilter.Builder()
                .setServiceUuid(new ParcelUuid(ThingyUtils.THINGY_BASE_UUID))
                .build());
        mThingyScanner.startScan(filters, settings, scanThingiesCallback);
        mIsScanningThingy = true;
        mScanresults.clear(); //clear list of available Thingies

        //timer to stop scan
        mThingyScannerHandler=new Handler();
        mThingyScannerHandler. postDelayed(mScanTimeoutRunnable,THINGY_SCAN_DURATION); //Timer for scanning duration
    }

    //scan timer expire -> ScanDone():
    final Runnable mScanTimeoutRunnable = new Runnable() {
        @Override
        public void run()  {
            mAutoDisConnect=false;
            ScanDone();
        }
    };

    /* finish scanning for avaible thingies
    result: List<ExtendedBluetoothDevice> mScanresults
     */
    private void ScanDone()
    {//finish scanning, display list and then start connecting to each Thingy
        String str1= "";
        stopScanThingies();
        mScanClusterButton.setText("Scan");

        if(mScanresults.size()<=0)
        {
            mClusterLogText.append("Scan Done, List: Empty\r\n");
        }
        else {
            //display avalable Thingies names to textbox (mClhLog in SoundFragment)
            mClusterLogText.append("Scan Done, List: \r\n");


            for (ExtendedBluetoothDevice result : mScanresults) {

                mClusterLogText.append(result.name + ", RSSI:" + result.rssi + ", times:" + result.detectedTimes + "\r\n");
            }
            mSaveScan=new ArrayList<>(mScanresults);
            enableAllClusterButtons();

        }
        enableAllClusterButtons();
    }

    public void stopScanThingies() {
        if (mIsScanningThingy) {
            mThingyScanner.stopScan(scanThingiesCallback);
            mThingyScannerHandler.removeCallbacks(mScanTimeoutRunnable);
            mIsScanningThingy = false;
        }
    }

    private final ScanCallback scanThingiesCallback = new ScanCallback() {
        /*this event call back for scanning an individual device,
         */
        @Override
        public void onScanResult(final int callbackType, @NonNull final ScanResult result) {
            Log.i(LOGTAG,"found 1 device call back");
        }

        /*this event call back for scanning multiple devices, which assigned in  startScanThingy
            It will add found devices into the List<ExtendedBluetoothDevice> mScanresults
        */
        @Override
        public void onBatchScanResults(final List<ScanResult> results) {

            ExtendedBluetoothDevice device;
            int pos;

            if (results.size() > 0) {
                for(ScanResult result:results)
                {//add scan results to the List
                    device= new ExtendedBluetoothDevice(result);
                    pos=mScanresults.indexOf(device);
                    if(pos==-1)
                    {//not yet in the list, update
                        mScanresults.add(device);
                    }
                    else
                    {//all ready in the list, average Rssi and number of detecting times
                        mScanresults.get(pos).updateDevice(result);
                    }
                    Log.i(LOGTAG,"find device:"+device.name + ",addr:" +device.device.getAddress());

                }
            }
        }
        @Override
        public void onScanFailed(final int errorCode) {
            // should never be called
        }
    };


    private final ScanCallback scanThingyCallback = new ScanCallback() {
        /*this event call back for scanning an individual device, which assigned in  startScanDevice
            If device is found, then it will stop scanning and connect to that device
         */
        @Override
        public void onScanResult(final int callbackType, @NonNull final ScanResult result) {

            final BluetoothDevice device = result.getDevice();
            //if (device.equals(mDevice)) {
            Log.i(LOGTAG,"scan current device finish, start timer");
            mClusterLogText.append("Found.\r\nConnecting...");
            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    Log.i(LOGTAG,"removeCallbacks(mThingyScannerRunable) in onScanResult");
                    mThingyScannerHandler.removeCallbacks(mThingyScannerRunable);//stop timeout timer
                    stopScanDevice();
                    new Handler().postDelayed(new Runnable() { //wait device stop scanning before connecting
                        @Override
                        public void run() {
                            Log.i(LOGTAG,"start connect "+device.getAddress()+" after delay 200 after finnish Scanning");
                            connect(device);
                        }
                    }, 200);
                }
            });
        }

        /*this event call back for scanning multiple devices
        this for HTC
        */
        @Override
        public void onBatchScanResults(final List<ScanResult> results) {
            Log.i(LOGTAG,"removeCallbacks(mThingyScannerRunable) in onScanResult");
            mThingyScannerHandler.removeCallbacks(mThingyScannerRunable);//stop timeout timer
            stopScanDevice();
            new Handler().postDelayed(new Runnable() { //wait device stop scanning before connecting
                @Override
                public void run() {
                    Log.i(LOGTAG,"start connect "+mDevice.getAddress()+" after delay 200 after finnish Scanning");
                    connect(mDevice);
                }
            }, 200);
        }
        @Override
        public void onScanFailed(final int errorCode) {
            // should never be called
        }
    };

    /*this method filters the list of devices resulted after finishing scanning for available Thingies
        return the approprated devices as List<BluetoothDevice>
    */
    private List<ExtendedBluetoothDevice> filterThingies(final List<ExtendedBluetoothDevice> devices)
    {

        List<ExtendedBluetoothDevice> retList=new ArrayList<>();

        if((devices==null)||(devices.size()<=0))
        {
            return retList;
        }

        List<ExtendedBluetoothDevice> tempList=new ArrayList<>();
        for(ExtendedBluetoothDevice device:devices)
        {//remove the devices with low Rssi and low detected times

            if(!device.validateDevice(1,-120)) tempList.add(device);
        }
        for(ExtendedBluetoothDevice device:tempList) {
            devices.remove(device);
        }


        // TODO: add your filter here, result should be return in a list:List<BluetoothDevice>
        //short Thingies in RSSI accend order
        List<ExtendedBluetoothDevice> tempRs=new ArrayList<ExtendedBluetoothDevice>(devices);
        List<ExtendedBluetoothDevice> readyThingy=new ArrayList<>();

        ExtendedBluetoothDevice maxBlT = null;
        int sz=devices.size();
        if(sz>MAX_CONNECTED_THINGIES)
        {
            sz=MAX_CONNECTED_THINGIES;
        }
        sz-=mThingySdkManager.getConnectedDevices().size();//max connectable devices- minus connected devices

        int max=-110;
        for(int i=0;i<sz;i++) {
            int ch=0;
            for (ExtendedBluetoothDevice result : tempRs) {
                if (ch == 0) {
                    max = result.rssi;
                    maxBlT=result;
                    ch = 1;
                } else {
                    if (result.rssi >= max) {
                        max = result.rssi;
                        maxBlT = result;
                    }
                }
            }
            //readyThingy.add(maxBlT);
            retList.add(maxBlT);
            tempRs.remove(maxBlT);
        }


       /* for(ExtendedBluetoothDevice device:readyThingy)
        {

                retList.add(device.device);
        }*/

        Log.i("ClhMain","ready list:" + retList.toString());
        return retList;
    }

    //connect to selected device in a List<BluetoothDevice>
    public void connectSelectedDevice(final List<ExtendedBluetoothDevice> devices, int index) {
        mExtDevice=devices.get(index);
        mDevice=mExtDevice.device; //assign this device to mDevice to use in other methods
        String str=mExtDevice.name;
        mClusterLogText.append("Start connecting to " + str + ":\r\n");
        Log.i("ClhMain","Start connecting to " + str);
        /*start scan to search for current device, then connect and discover each Thingy
        The procedure will go through these steps:
        - mScancallback#onScanResult: if device found, then start connecting to the device
        - Then wait for callback via:
             + ThingyListener#onDeviceConnected()
              -> ThingyListener#onServiceDiscoveryCompleted()
              -> nextThingy
        - or time out in mThingyScannerRunable -> nextThingy
        --*/
        stopScanDevice();
        mClusterLogText.append("Searching...");
        startScanDevice(mExtDevice);
        Log.i(LOGTAG,"start(mThingyScannerRunable)");
        mThingyScannerHandler.postDelayed(mThingyScannerRunable,SCAN_DURATION); //timer for connect and discover each device
    }

    /*fail connecting to a device,
     stop scanning or disconnect to current device, continue next one
    */
    final Runnable mThingyScannerRunable = new Runnable() {
        @Override
        public void run()  {
            Log.i(LOGTAG, "Scan timeout");

            if(mIsScanningThingy) stopScanDevice();
            else if(mThingySdkManager.isConnected(mDevice)) {
                //device connected but not yet finished discovering, then disconnect
                mThingySdkManager.disconnectFromThingy(mDevice);
            }
            nextThingy(false);
        }
    };

    /*connect to next Thingy in the mAvaibleThingies list, or wrap up when finishing all items
    input:
        previousResult: for displaying and processing next step
            true: previous device succesfully connected, then try next one
            false: previous device failed, retry or next one
       mAvaibleThingies: list of Bluetooth devices to be connected
       mThingyListIndex: index of current device
    */
    private void nextThingy(final boolean previousResult)
    {
        //display
        if(previousResult)
        {
            mClusterLogText.append("Device:" +mExtDevice.name +" Done. \r\n");
        }
        else{
            mClusterLogText.append(": Failed \r\n");
            mClusterLogText.append("Device:" +mExtDevice.name +" Failed. \r\n");

        }
        Log.i(LOGTAG,"removeCallbacks(mThingyScannerRunable) in nextThingy");
        mThingyScannerHandler.removeCallbacks(mThingyScannerRunable); //stop time out timer of previous device
        Handler handler=new Handler();
        handler.postDelayed(new Runnable() {//delay for finishing send notification (MTU) of previous trial
            @Override
            public void run() {
                if((previousResult==false)&&(mRetryConnecting==false)){ //previous device fail for 1st time, retry
                    mRetryConnecting=true ; //retry one more time
                }
                else {//next item index
                    mRetryConnecting=false;
                    mThingyListIndex++;
                }
                mThingyListIndex=selectNextThingies(mThingyListIndex, mAvaibleThingies);
                //find a device to connect
                if(mThingyListIndex!=-1)
                    connectSelectedDevice(mAvaibleThingies, mThingyListIndex); //connect next device
                else {
                    //finished all items
                    //
                    //broad cast data to network
                    //PSnote broadcastCluster();
                    mAutoConnect=false;
                    enableAllClusterButtons();// ->enable sound stream of each the back to Sound Fragment
                    List<BluetoothDevice> connectDevices = mThingySdkManager.getConnectedDevices();
                    if (connectDevices.size() > 0) {
                        Log.i(LOGTAG, "Connected List:");
                        mClusterLogText.append("Connected devices:");
                        for (BluetoothDevice dv : connectDevices) {
                            mThingySdkManager.enableThingyMicrophone(dv, true,THINGY_MICROPHONE_SAMPLING_FREQ, true); //enable sound stream
                            int color=colorDefine.get("blue");
                            int red=(color&0x00FF0000)>>16;
                            int green=(color&0x0000FF00)>>8;
                            int blue=(color&0x000000FF);
                            mThingySdkManager.setConstantLedMode(dv,red,green,blue);
                        }
                        for (ExtendedBluetoothDevice exdv:mConnectedBleDeviceList)
                        {
                            mClusterLogText.append(exdv.name+", ");
                            Log.i(LOGTAG, "Connected device:" + exdv.name);
                        }

                        mSaveSoundButton.setEnabled(true);
                        mClusterLogText.append("\r\n");

                        //init sound var
                        mSoundVar.clear();
                        mSoundVarCount.clear();
                        int i=0;
                        for (BluetoothDevice dv : connectDevices) {
                            mSoundVar.put(dv.getAddress(),mSoundArr[i]);
                            mSoundVarCount.put(dv.getAddress(),0);
                            i++;
                        }
                    }
                }
            }
        },100);
    }

    @SuppressWarnings("MissingPermission")
    private String createSaveFiles(BluetoothDevice dv) {
        Timestamp currentTime = new Timestamp(System.currentTimeMillis());
        String addr = dv.getAddress();
        Log.i("Test1", addr);
        String name = dv.getName() + "_" + addr + "_" + currentTime + ".pcm";
        Log.i("Test1", name);
        File fname = new File(pathSoundData, name);
        soundFileName.put(dv.getAddress(), fname);
        //soundFileName[i]=new File(pathSoundData,name);
        if (!fname.exists()) {
            try {
                fname.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return name;
    }
     /*select the an available (not connected yet) Thingy in list
    input:
       List<BluetoothDevice> deviceList: list of Bluetooth devices to be connected
       int currentIndex: index of current device
    return: index of the available one in the list
    -1: if not found any one, or the number of the connected ones are exceed the MAX_THINGIES_CLUSTER
     0..MAX_THINGIES_CLUSTER-1: if found
    */

    private int selectNextThingies(final int currentIndex, final List<ExtendedBluetoothDevice> deviceList)
    {
        int selection=-1;
        int ind=currentIndex;
        List<BluetoothDevice> connectedDevice=mThingySdkManager.getConnectedDevices();
        ExtendedBluetoothDevice extdevice;
        BluetoothDevice dv;

        if((deviceList==null)||(deviceList.size()<=0)){ return selection;}

        while((ind<deviceList.size())&&(ind<MAX_THINGIES_CLUSTER))
        {
            extdevice=deviceList.get(ind);
            if(connectedDevice.contains(extdevice.device)){//if current device is connected, go to next
                ind++;
            }
            else
            {
                selection=ind;
                break;
            }
        }
        return selection;
    }


    private void startScanDevice(ExtendedBluetoothDevice extdevice) {
        BluetoothDevice device=extdevice.device;
        if (mIsScanningThingy) {
            return;
        }
        final ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setReportDelay(0)
                .setUseHardwareBatchingIfSupported(false)
                .setUseHardwareFilteringIfSupported(true)
                .build();
        final List<ScanFilter> filters = new ArrayList<>();
        filters.add(new ScanFilter.Builder()
                .setDeviceAddress(device.getAddress())
                .build());
        mThingyScanner.startScan(filters, settings, scanThingyCallback);
        mIsScanningThingy = true;
        Log.i(LOGTAG,"Start scan device:"+extdevice.name + ",addr:"+ device.getAddress().toString());
    }

    private void connect() {
        mThingySdkManager.connectToThingy(this, mDevice, ThingyService.class);
        mThingySdkManager.setSelectedDevice(mDevice);
    }

    private void connect(final BluetoothDevice device) {
        mThingySdkManager.connectToThingy(this, device, ThingyService.class);
        mThingySdkManager.setSelectedDevice(device);
    }

    @SuppressWarnings("MissingPermission")
    private void disConnectAllThingies()
    {
        List<BluetoothDevice> connectDevices= mThingySdkManager.getConnectedDevices();



        mAutoDisConnect=true; //used in callback mThingyListerner::onDeviceDisconnected
        stopScanDevice();
        stopScanThingies();
        mClusterLogText.append("Disconnecting all devices \r\n");

        //first disconnect all connected Thingies
        if(connectDevices.size()>0)
        {
            Log.i(LOGTAG,"Disconecting all devices.");

            //start disconnect first device
            //-> call back in mThingyListerner::onDeviceDisconnected
            Log.i(LOGTAG,"Disconnect device:" + connectDevices.get(0).getName());
            mThingySdkManager.disconnectFromThingy(connectDevices.get(0));
            mThingyScannerHandler=new Handler();
            mThingyScannerHandler.postDelayed(waitThingyDisconnect,4000); //timeout for waiting all Thingies disconnected
        }
    }

    Runnable waitThingyDisconnect=new Runnable() {//time out for disconnecting all Thingies
        @Override
        public void run() {
            onFinishDiconnectAll();
        }
    };

    private void onFinishDiconnectAll(){
        ///finished disconnecting all devices
        mClusterLogText.append(":Done\r\n");
        enableAllClusterButtons();
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //ViewDataBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_main);



        // Ensure that Bluetooth exists
        if (!ensureBleExists())
            finish();

        if (checkIfRequiredPermissionsGranted()) {
            if (!isLocationEnabled()) {
                 final MessageDialogFragment messageDialogFragment = MessageDialogFragment.newInstance(getString(R.string.location_services_title), getString(R.string.rationale_message_location));
                messageDialogFragment.show(getSupportFragmentManager(), null);
            }
        }




        mThingySdkManager = ThingySdkManager.getInstance();

        mScanClusterButton=findViewById(R.id.scanClusterButton);
        mScanClusterButton.setText("Scan");
        mConnectThingyButton=findViewById((R.id.connectThingyButton));
        mDisconnectThingButton=findViewById(R.id.disconnectThingyButton);
        mSaveSoundButton=findViewById(R.id.saveSoundButton);
        mClusterLogText=findViewById(R.id.txtLogCluster);
        mConnectedBleDeviceList = new ArrayList<>();
        mtxtClhNameInp=findViewById(R.id.txtName);


       // ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(MainActivity.this, R.array.Tx_power,android.R.layout.simple_spinner_item);

        //initial Clusterhead: advertiser, scanner, processor


        mStartClh=findViewById(R.id.StartClhButton);
        mClearLog=findViewById(R.id.clearLogClhButton);
        mClhLogText=findViewById(R.id.clhLogtext);
        mSoundEventLog=findViewById(R.id.logSoundEvent);
        mSoundEventLog.setMovementMethod(new ScrollingMovementMethod());

        mStartClh.setText("Start");
        mIsSinkCheckBox=findViewById(R.id.isSinkCheck);
        mClhIDTextBox=findViewById(R.id.clusterHeadIDtext);
        mClhIDTextBox.setFilters(new InputFilter[]{ new InputFilterMinMax("0", "126")});
        mIsAdvertising=false;

        mTxPowerSelect=findViewById(R.id.selectTxPowerSpinner);
        mTxPowerSelect.setSelection(ClhParams.CLH_TX_POWER_HIGH);

        mClhParams=new ClhParams();
        mClh=new ClusterHead(mClhParams);
        mClh.initClhBLE();
        mClhAdvertiser=mClh.getClhAdvertiser();
        mClhScanner=mClh.getClhScanner();
        mClhProcessor=mClh.getClhProcessor();

        mClearLog.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
               mClhLogText.setText("");


              /* Intent intent = new Intent(MainActivity.this,
                       SoundFileMenu.class);
               intent.putExtra("Text","hello");
               startActivity(intent);*/

//Pstest

              /*for(int i=0;i<20;i++) {
                   byte[] soundtest = null;

                   String strName="faucet"+String.format("%2d",i);
                   try {
                       soundtest = readWav(i);
                   } catch (IOException e) {
                       e.printStackTrace();
                   }
                   float[] dt=getMFCCdata(soundtest);
                   Float[] mfcc=new Float[dt.length];
                   for(int j=0;j<dt.length;j++)
                   {
                       mfcc[j]=dt[j];
                   }
                   String str=soundClassify(mfcc,"12:34:56:78:12:23");
                   Log.i(LOGTAG,"mfcc:"+Arrays.toString(dt));
                   Log.i(LOGTAG,"soundClassi:"+i+":"+str);


               }
               byte[] soundtest = null;


               SoundObj sound=new SoundObj(soundtest,16000,512,0.02f,0.01f,-54,3);
               double[][] sp1=sound.spectrogram();

               float[] mfcc1=getMFCCdata(soundtest);
               Log.i(LOGTAG,Arrays.toString(mfcc1));
               Float[] mfcc2=new Float[mfcc1.length];
               for(int j=0;j<mfcc1.length;j++)
               {
                   mfcc2[j]=mfcc1[j];
               }
               soundClassify(mfcc2,"12:34:56:78:12:23");*/
           }
         });

        mStartClh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mStartClh.getText().toString()=="Start") {
                    mStartClh.setText("Stop");

                    //set cluster head name
                    String txtname= mtxtClhNameInp.getText().toString();
                    if (txtname.length()!=0)
                    {
                        mClh.setClhName(txtname);
                    }


                    ClhParams params=getClusterHeadSettings();
                    mClhAdvertiser.setClhParams(params);
                    mClhScanner.setClhParams(params);
                    mIsAdvertising=true;
                    mClhIDTextBox.setEnabled(false);
                    mIsSinkCheckBox.setEnabled(false);
                    mTxPowerSelect.setEnabled(false);
                    //TODO: set params here
                    if((mClhID<=126)&&(mClhID>=100)) {
                        //mClhID = 1;
                        ClhAdvertisedData clhData=new ClhAdvertisedData();

                        byte t1=0;
                        byte[] x= {1,2,3,4,5,6,7,8,9,10};
                        for(int i=0;i<64;i++)
                        {
                            clhData.setSourceID(mClhID);
                            clhData.setDestId((byte)0);
                            clhData.setHopCount((byte)1);
                            clhData.setNextHopId((byte)99);
                            x[9]=t1++;
                            clhData.setUserData(x);
                            mClhAdvertiser.addAdvPacketToBuffer(clhData,true);
                        }
                    }
                    mClhAdvertiser.nextAdvertisingPacket(); //start advertising
                    mClhScanner.clhScanStart();

                }
                else
                {
                    mStartClh.setText("Start");
                    mIsAdvertising=false;

                    mClhAdvertiser.stopClhAdvertiser();
                    mClhScanner.stopScanCLH();

                    mIsSinkCheckBox.setEnabled(true);
                    if(!mIsSinkCheckBox.isChecked())   mClhIDTextBox.setEnabled(true);
                    mTxPowerSelect.setEnabled(true);
                }
            }
        });


        mScanClusterButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if(mScanClusterButton.getText().toString()=="Scan")
                {
                    startScanThingies();
                    mScanClusterButton.setText("Stop");
                    mConnectThingyButton.setEnabled(false);
                    mDisconnectThingButton.setEnabled(false);
                }
                else {
                    stopScanThingies();
                    mScanClusterButton.setText("Scan");
                }
            }
        });

        mConnectThingyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {




                mAutoConnect=true;
                //set up timer for each packet advertising, expire interval in mAdvInterval
 /*               mReconnThingyTimer=new CountDownTimer(60000*60,30000) {
                    @Override
                    public void onTick(long millisUntilFinished) {//tick, not used
                        if(mAutoConnect==true) {
                            if (mThingySdkManager.getConnectedDevices().size() < MAX_CONNECTED_THINGIES){
                                mThingyListIndex=selectNextThingies(0, mAvaibleThingies); //find a device to connect
                                if(mThingyListIndex!=-1){
                                    connectSelectedDevice(mAvaibleThingies,mThingyListIndex); //connect next device
                                    disableAllClusterButtons();
                                }

                            };
                        }

                    }

                    @Override
                    public void onFinish() {//timer expire, advertising next packet
                        if(mAutoConnect==true) mReconnThingyTimer.start();
                    }
                }.start();*/


                mAvaibleThingies =filterThingies(mSaveScan); //get the list to be connected
                /*---------connect to first device
                next devices will be connect through
                - callback of finishing current device via:
                     ThingyListener#onDeviceConnected()
                      -> ThingyListener#onDeviceConnected()#onServiceDiscoveryCompleted
                      -> nextThingy
                - or time out in mThingyScannerRunable -> nextThingy
                ---------*/
                mThingyListIndex=selectNextThingies(0, mAvaibleThingies); //find a device to connect
                if(mThingyListIndex!=-1){
                    connectSelectedDevice(mAvaibleThingies,mThingyListIndex); //connect next device
                    disableAllClusterButtons();
                }
                else{
                    showToast(MainActivity.this,("Device list is empty or full"));
                }
            }
        });

        mDisconnectThingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<BluetoothDevice> connectDevices= mThingySdkManager.getConnectedDevices();
                mAutoConnect=false;
                if(mReconnThingyTimer!=null) mReconnThingyTimer.cancel();
                if(connectDevices.size()>0) {
                    disableAllClusterButtons();
                    stopScanThingies();
                    stopScanDevice();
                    disConnectAllThingies();
                    if(mSaveSoundButton.getText()==getString(R.string.stopSave))
                         mSaveSoundButton.performClick();
                    mSaveSoundButton.setEnabled(false);
                }
                else
                {
                    showToast(MainActivity.this,("No connected device"));
                }
            }
        });

        mSaveSoundButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick (View v){
                List<BluetoothDevice> connectDevices = mThingySdkManager.getConnectedDevices();
                if(mSaveSoundButton.getText()==getString(R.string.startSave))
                {//start save sound data to file .pcm
                    mSaveSoundButton.setText(getString(R.string.stopSave));

                    soundFileName.clear();
                    for (BluetoothDevice dv : connectDevices) {
                        String str=createSaveFiles(dv);
                        mSoundEventLog.append("PCM:"+str+"\r\n");
                        mSaveEnable=true;
                    }
                }
                else
                {//convert file .pcm -> wav
                    mSaveSoundButton.setText(getString(R.string.startSave));
                    mSaveEnable=false;
                    for (String key:soundFileName.keySet()) {
                        File file=soundFileName.get(key);
                        String name=file.getName();
                        String name2=name.replace(".pcm",".wav");
                        File file2= new File(pathSoundData,name2);
                        soundFileNameWav.put(key,file2);
                        try {
                            PCMtoWAV.convert(file,file2,1,THINGY_MICROPHONE_SAMPLING_FREQ,16);
                            mSoundEventLog.append("WAV:"+file2.getName()+"\r\n");

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    //init sound var
                    mSoundVar.clear();
                    mSoundVarCount.clear();
                    int i=0;
                    for (BluetoothDevice dv : connectDevices) {
                        mSoundVar.put(dv.getAddress(),mSoundArr[i]);
                        mSoundVarCount.put(dv.getAddress(),0);
                        i++;
                    }        final Handler handler=new Handler();
                    handler. postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            handler.postDelayed(this, 1000); //loop every cycle
                            ArrayList<ClhAdvertisedData> procList=mClhProcessor.getProcessDataList();
                            for(int i=0; i<procList.size();i++)
                            {
                                if(i==10) break; //just display 10 line in one cycle

                                ClhAdvertisedData data =procList.get(0);
                                byte[] dataArr=data.getParcelClhData();
                                mClhLogText.append("Pr:");
                                mClhLogText.append(Arrays.toString(dataArr));
                                mClhLogText.append("\r\n");
                                procList.remove(0);
                            }
                            ArrayList<ClhAdvertisedData> fwdList=mClhScanner.getprintForwardList();
                            for(int i=0; i<fwdList.size();i++)
                            {
                                if(i==10) break; //just display 10 line in one cycle

                                ClhAdvertisedData data =fwdList.get(0);
                                byte[] dataArr=data.getParcelClhData();
                                mClhLogText.append("Fw:");
                                mClhLogText.append(Arrays.toString(dataArr));
                                mClhLogText.append("\r\n");
                                fwdList.remove(0);
                            }

                            ArrayList<ClhAdvertisedData> sendList=mClhAdvertiser.getprintSendList();
                            for (int i = 0; i < sendList.size(); i++) {
                                if (i == 10) break; //just display 10 line in one cycle

                                ClhAdvertisedData data = sendList.get(0);
                                byte[] dataArr = data.getParcelClhData();
                                mClhLogText.append("S:");
                                mClhLogText.append(Arrays.toString(dataArr));
                                mClhLogText.append("\r\n");
                                sendList.remove(0);
                            }


                        }
                    }, 1000); //the time you want to delay in milliseconds
                }
            }
        });

        mIsSinkCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            private final String mClhIDCurrent="12345";

            @Override
            public void onCheckedChanged(CompoundButton buttonView,boolean isChecked) {
                if(mIsSinkCheckBox.isChecked()){
                    mClhIDTextBox.setText("0");
                    mClhIDTextBox.setEnabled(false);
                }
                else
                {
                    mClhIDTextBox.setText("1");
                    mClhIDTextBox.setEnabled(true);
                }
            }
        });


        final Handler handler=new Handler();
        handler. postDelayed(new Runnable() {
            @Override
            public void run() {
                handler.postDelayed(this, 1000); //loop every cycle
                ArrayList<ClhAdvertisedData> procList=mClhProcessor.getProcessDataList();
                    for(int i=0; i<procList.size();i++)
                    {
                        if(i==10) break; //just display 10 line in one cycle

                        ClhAdvertisedData data =procList.get(0);
                        byte[] dataArr=data.getParcelClhData();
                        mClhLogText.append("Pr:");
                        mClhLogText.append(Arrays.toString(dataArr));
                        mClhLogText.append("\r\n");
                        procList.remove(0);
                    }
                ArrayList<ClhAdvertisedData> fwdList=mClhScanner.getprintForwardList();
                for(int i=0; i<fwdList.size();i++)
                {
                    if(i==10) break; //just display 10 line in one cycle

                    ClhAdvertisedData data =fwdList.get(0);
                    byte[] dataArr=data.getParcelClhData();
                    mClhLogText.append("Fw:");
                    mClhLogText.append(Arrays.toString(dataArr));
                    mClhLogText.append("\r\n");
                    fwdList.remove(0);
                }

                ArrayList<ClhAdvertisedData> sendList=mClhAdvertiser.getprintSendList();
                for (int i = 0; i < sendList.size(); i++) {
                    if (i == 10) break; //just display 10 line in one cycle

                    ClhAdvertisedData data = sendList.get(0);
                    byte[] dataArr = data.getParcelClhData();
                    mClhLogText.append("S:");
                    mClhLogText.append(Arrays.toString(dataArr));
                    mClhLogText.append("\r\n");
                    sendList.remove(0);
                }


            }
        }, 1000); //the time you want to delay in milliseconds

        pathSoundData=new File(getFilesDir(),"soundData");
        pathSoundData.mkdirs();
        Log.i("Test1","Sound data folder"+  pathSoundData);

    }



    private void enableAllClusterButtons()
    {
        mDisconnectThingButton.setEnabled(true);
        mConnectThingyButton.setEnabled(true);
        mScanClusterButton.setEnabled(true);
    }
    private void disableAllClusterButtons()
    {
        mDisconnectThingButton.setEnabled(false);
        mConnectThingyButton.setEnabled(false);
        mScanClusterButton.setEnabled(false);
    }

    @SuppressWarnings("MissingPermission")
    private final ThingyListener mThingyListener = new ThingyListener() {
        private final Handler mHandler = new Handler();
        @Override
        public void onDeviceConnected(BluetoothDevice device, int connectionState) {
            Log.i(LOGTAG,"device connected in onDeviceConnected");
            mClusterLogText.append(":Connected.\r\n Discovering...");

            if (!mConnectedBleDeviceList.contains(mExtDevice)) {
                mConnectedBleDeviceList.add(mExtDevice);
            }

            if(mAutoConnect){//use for auto connect
                Log.i(LOGTAG,"device connected in 1");

            }
            else
            {//use for connect one Thingy
                Log.i(LOGTAG,"device connected in 2");

            }

        }

        @Override
        @SuppressWarnings("MissingPermission")
        public void onDeviceDisconnected(BluetoothDevice device, int connectionState) {
            Log.i(LOGTAG,"device disconect");
            mClusterLogText.append("**"+device.getName() + " disconnected** \r\n");

            //remove device from ext connected list
            for(ExtendedBluetoothDevice exdv:mConnectedBleDeviceList)
            {
                if (exdv.device.getAddress().equals(device.getAddress()))
                {
                    mConnectedBleDeviceList.remove(exdv);
                    break;
                }
            }
            enableAllClusterButtons(); //todo: a bug here to enable buttons while connecting
            if(mAutoConnect){
                nextThingy(false); //reconnect
            }
            else {

                if (mAutoDisConnect)//use for auto disconnect all Thingies
                {
                    List<BluetoothDevice> devices = mThingySdkManager.getConnectedDevices();

                    if (devices.size() > 0) {
                        Log.i(LOGTAG, "Disconnecting " + devices.get(0).getName());
                        mThingySdkManager.disconnectFromThingy(devices.get(0));
                    } else {
                        mAutoDisConnect = false;
                        mThingyScannerHandler.removeCallbacks(waitThingyDisconnect);
                        Log.i(LOGTAG, "All devices were Disconnected");
                        onFinishDiconnectAll();
                    }
                } else {//use for disconnet individual THingy
                    mClusterLogText.append("** Connected devices:");

                    for (int i = 0; i < mThingySdkManager.getConnectedDevices().size(); i++)
                        mClusterLogText.append(mThingySdkManager.getConnectedDevices().get(i).getName() + ", ");
                    mClusterLogText.append("** \r\n");

                }
            }
        }



        @Override
        @SuppressWarnings("MissingPermission")
        public void onServiceDiscoveryCompleted(BluetoothDevice device) {
            Log.i(LOGTAG,"discovery complete");
            mClusterLogText.append(":Discovered. \r\n Send Notification...");
            mThingySdkManager.enableBatteryLevelNotifications(device, true);

            enableSoundNotifications(device, true); //enable receive sound data
            enableUiNotifications();
            int color=colorDefine.get("blue");
            int red=(color&0x00FF0000)>>16;
            int green=(color&0x0000FF00)>>8;
            int blue=(color&0x000000FF);
            mThingySdkManager.setConstantLedMode(device,red,green,blue);

            //enableEnvironmentNotifications();
            //enableMotionNotifications();

            if(!mWaitingMtuResponse)
            {//mWaitingMtuResponse is set in enableSoundNotifications, to change to MTU of BLE from 23 to 276
                // if not using sound, the default MTU is 23, and go to next Thingy without waiting for MTU change
                if(mAutoConnect){
                    nextThingy(true);
                }
            }
            else
            {//if using sound data, wait for MTU changing response from Thingy -> onMtuChangingCompleted

            }

        }

        @Override
        public void onMtuChangingCompleted(BluetoothDevice device, int mtu){
            Log.i(LOGTAG,"MTU changed" + mtu);
            mClusterLogText.append("Done...\r\n");
            if(mAutoConnect){
                nextThingy(true);
            }
        }

        @Override
        public void onBatteryLevelChanged(final BluetoothDevice bluetoothDevice, final int batteryLevel) {
            Log.v(ThingyUtils.TAG, "Battery Level: " + batteryLevel + "  address: " + bluetoothDevice.getAddress() + " name: " + bluetoothDevice.getName());
        }

        @Override
        public void onTemperatureValueChangedEvent(BluetoothDevice bluetoothDevice, String temperature) {

        }

        @Override
        public void onPressureValueChangedEvent(BluetoothDevice bluetoothDevice, String pressure) {

        }

        @Override
        public void onHumidityValueChangedEvent(BluetoothDevice bluetoothDevice, String humidity) {

        }

        @Override
        public void onAirQualityValueChangedEvent(BluetoothDevice bluetoothDevice, final int eco2, final int tvoc) {

        }

        @Override
        public void onColorIntensityValueChangedEvent(BluetoothDevice bluetoothDevice, float red, float green, float blue, float alpha) {

        }





        @Override
        public void onButtonStateChangedEvent(BluetoothDevice bluetoothDevice, final int buttonState) {
        }

        @Override
        public void onTapValueChangedEvent(final BluetoothDevice bluetoothDevice, final int direction, final int count) {

        }

        @Override
        public void onOrientationValueChangedEvent(final BluetoothDevice bluetoothDevice, final int orientation) {

        }

        @Override
        public void onQuaternionValueChangedEvent(final BluetoothDevice bluetoothDevice, float w, float x, float y, float z) {

        }

        @Override
        public void onPedometerValueChangedEvent(final BluetoothDevice bluetoothDevice, final int steps, final long duration) {

        }

        @Override
        public void onAccelerometerValueChangedEvent(final BluetoothDevice bluetoothDevice, final float x, final float y, final float z) {

        }

        @Override
        public void onGyroscopeValueChangedEvent(final BluetoothDevice bluetoothDevice, final float x, final float y, final float z) {

        }

        @Override
        public void onCompassValueChangedEvent(final BluetoothDevice bluetoothDevice, final float x, final float y, final float z) {

        }

        @Override
        public void onEulerAngleChangedEvent(final BluetoothDevice bluetoothDevice, final float roll, final float pitch, final float yaw) {

        }

        @Override
        public void onRotationMatrixValueChangedEvent(final BluetoothDevice bluetoothDevice, final byte[] matrix) {

        }

        @Override
        public void onHeadingValueChangedEvent(final BluetoothDevice bluetoothDevice, final float heading) {

        }

        @Override
        public void onGravityVectorChangedEvent(final BluetoothDevice bluetoothDevice, final float x, final float y, final float z) {

        }

        @Override
        public void onSpeakerStatusValueChangedEvent(final BluetoothDevice bluetoothDevice, final int status) {

        }

        @Override
        public void onMicrophoneValueChangedEvent(final BluetoothDevice dv, final byte[] data) {

            if (data != null) {
                if (data.length != 0) {
                    mHandler.post(new Runnable() {
                        @RequiresApi(api = Build.VERSION_CODES.O)
                        @Override
                        public void run() {
                            //mVoiceVisualizer.draw(data);
                            //long time2=currentTimeMillis()-time1;
                            //time1=currentTimeMillis();
                            //Log.i(LOGTAG, "process sound data of " + dv.getName() + " data size:" + data.length + ",duration:"+time2);
                            if(mSaveEnable==true){
                                File file=soundFileName.get(dv.getAddress());
                                FileOutputStream writer = null;
                                try {
                                    writer = new FileOutputStream(file, true);
                                    writer.write(data);
                                    writer.flush();
                                    writer.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                            else {
                                String addr=dv.getAddress();
                                Integer cnt=mSoundVarCount.get(addr);
                                System.arraycopy(data, 0, mSoundVar.get(addr), cnt*512, 512);
                                if (++cnt<NUM_OF_SOUND_PACKET)
                                {
                                    mSoundVarCount.put(addr,cnt);
                                }
                                else
                                {
                                    cnt=0;
                                    //Log.i("Test2",Arrays.toString(mSoundVar.get(dv.getAddress())));

                                    //Todo: call process data here

                                            byte[] data2=mSoundVar.get(addr);
                                            String str=processSoundData(data2,addr);
 /*                                     if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
                                    }
                                    ;
                                    LocalTime localDate1 = LocalTime.now();

                                   //PStest Float[] mfcc1=getMFCCdata(data2);
                                    byte[] data3=Arrays.copyOfRange(data2,0,16000);
                                    float[] dt=getMFCCdata(data3);
                                    Float[] mfcc1=new Float[dt.length];
                                    for(int j=0;j<dt.length;j++)
                                    {
                                        mfcc1[j]=dt[j];
                                    }

                                    String clRs=soundClassify(mfcc1,addr);
                                            Log.i("MFCC",Arrays.toString(mfcc1));
                                    LocalTime localDate2 = LocalTime.now();
                                    Log.i("MFCC","Start:"+localDate1.toString());
                                    Log.i("MFCC","Done:"+localDate2.toString());*/



                                    String[] tokens = str.split("LED color:");
                                            String verbal=tokens[0];
                                            Log.i("Test3", verbal);
                                            if (tokens.length>=2) {//get color
                                                Log.i("Test3", tokens[1] );
                                                String[] tokens2=tokens[1].split("toSink:");

                                                String[] tokens3 = tokens2[0].split("@");
                                                String strLed=tokens3[0].trim();
                                                String macAddr=(tokens3[1].trim()).replace("\r\n","");
                                                if(!strLed.equals("unchanged"))
                                                {
                                                    mSoundEventLog.append(verbal); //display verbal in mSoundEventLog textbox
                                                    int color=colorDefine.get(strLed);
                                                    int red=(color&0x00FF0000)>>16;
                                                    int green=(color&0x0000FF00)>>8;
                                                    int blue=(color&0x000000FF);
                                                    mThingySdkManager.setConstantLedMode(dv,red,green,blue);
                                                }
                                                if(tokens2.length>=2)
                                                {
                                                    String strAdv=(tokens2[1].trim()).replace("\r\n","");
                                                    byte[] dataAdv=hexStringToByteArray(strAdv);
                                                    if (mIsAdvertising) {
                                                        int t=mClhAdvertiser.addAdvSoundData(dataAdv);
                                                        if (t==0) mClhLogText.append("Err\r\n");

                                                    }
                                                }
                                            }
                                }
                                mSoundVarCount.put(dv.getAddress(),cnt);



                            }
                        }
                    });
                }
            }
        }
    };
    //long time1=currentTimeMillis();
  /*  private void writeAudioDataToFile() {
        // Write the output audio in byte
        int BufferElements2Rec = 1024; // want to play 2048 (2K) since 2 bytes we
        // use only 1024
        int BytesPerElement = 2; // 2 bytes in 16bit format

        String filePath = "/sdcard/voice8K16bitmono.pcm";
        short sData[] = new short[BufferElements2Rec];

        FileOutputStream os = null;
        try {
            os = new FileOutputStream(filePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        while (isRecording) {
            // gets the voice output from microphone to byte format

            recorder.read(sData, 0, BufferElements2Rec);
            System.out.println("Short wirting to file" + sData.toString());
            try {
                // // writes the data to file from buffer
                // // stores the voice buffer

                byte bData[] = short2byte(sData);

                os.write(bData, 0, BufferElements2Rec * BytesPerElement);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
*/

    private void enableUiNotifications() {
        mThingySdkManager.enableButtonStateNotification(mDevice, true);
    }
    public void enableSoundNotifications(final BluetoothDevice device, final boolean flag) {
        if (mThingySdkManager != null) {
            mWaitingMtuResponse=true;
            mThingySdkManager.requestMtu(device);
            mThingySdkManager.enableSpeakerStatusNotifications(device, flag);
        }
    }

    private void stopScanDevice() {
        if (mBinder != null) {
            mBinder.setScanningState(false);
        }
        if (mIsScanningThingy) {
            Log.i(LOGTAG, "Stopping scan original");
            mThingyScanner.stopScan(scanThingyCallback);
            Log.i(LOGTAG,"removeCallbacks(mThingyScannerRunable) in stopScanDevice");
            mThingyScannerHandler.removeCallbacks(mThingyScannerRunable);
            mIsScanningThingy = false;
        }
    }

    final BroadcastReceiver mBleStateChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final String action = intent.getAction();
            if (action != null && action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                if (state == BluetoothAdapter.STATE_OFF) {
                    Utils.showToast(MainActivity.this, getString(R.string.ble_turned_off));
                    enableBle();
                }
            }
        }
    };
    /**
     * Checks whether the device supports Bluetooth Low Energy communication
     *
     * @return <code>true</code> if BLE is supported, <code>false</code> otherwise
     */
    private boolean ensureBleExists() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.no_ble, Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }
    /**
     * Tries to start Bluetooth adapter.
     */
    @SuppressWarnings("MissingPermission")
    private void enableBle() {
        final Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableIntent, Utils.REQUEST_ENABLE_BT);
    }
    /**
     * Checks whether the Bluetooth adapter is enabled.
     */
    private boolean isBleEnabled() {
        final BluetoothManager bm = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        final BluetoothAdapter ba = bm.getAdapter();
        return ba != null && ba.isEnabled();
    }
    /**
     * Since Marshmallow location services must be enabled in order to scan.
     *
     * @return true on Android 6.0+ if location mode is different than LOCATION_MODE_OFF. It always returns true on Android versions prior to Marshmellow.
     */
    @SuppressWarnings("MissingPermission")
    public boolean isLocationEnabled() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int locationMode = Settings.Secure.LOCATION_MODE_OFF;
            try {
                locationMode = Settings.Secure.getInt(getContentResolver(), Settings.Secure.LOCATION_MODE);
            } catch (final Settings.SettingNotFoundException e) {
                // do nothing
            }
            return locationMode != Settings.Secure.LOCATION_MODE_OFF;
        }
        return true;
    }
    @Override
    protected void onStart() {
        super.onStart();
        if (!isBleEnabled()) {
            enableBle();
        }

        if (!isLocationEnabled()) {
            Log.i(LOGTAG,"no location");
        } else {
            Log.i(LOGTAG,"have location");
        }
        mThingySdkManager.bindService(this, ThingyService.class);
        ThingyListenerHelper.registerThingyListener(this, mThingyListener);
        registerReceiver(mBleStateChangedReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));

    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mBinder != null) {
            final boolean isFinishing = isFinishing();
            mBinder.setActivityFinishing(isFinishing);
        }
        stopScanThingies();
        stopScanDevice();
        mClhAdvertiser.stopClhAdvertiser();
        mClhScanner.stopScanCLH();
        mThingySdkManager.unbindService(this);
        mBinder = null;
        ThingyListenerHelper.unregisterThingyListener(this, mThingyListener);
        unregisterReceiver(mBleStateChangedReceiver);
    }

    @Override
    public void onServiceConnected() {
        //Use this binder to access you own API methods declared in the binder inside ThingyService
        mBinder = (ThingyService.ThingyBinder) mThingySdkManager.getThingyBinder();
    }


    private boolean checkIfRequiredPermissionsGranted() {
        if (checkIfVersionIsQ()) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                final PermissionRationaleDialogFragment dialog = PermissionRationaleDialogFragment.getInstance(Manifest.permission.ACCESS_FINE_LOCATION, REQUEST_ACCESS_FINE_LOCATION, getString(R.string.rationale_message_location));
                dialog.show(getSupportFragmentManager(), null);
                return false;
            }
        } else if (checkIfVersionIsMarshmallowOrAbove()) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                final PermissionRationaleDialogFragment dialog = PermissionRationaleDialogFragment.getInstance(Manifest.permission.ACCESS_COARSE_LOCATION, REQUEST_ACCESS_COARSE_LOCATION, getString(R.string.rationale_message_location));
                dialog.show(getSupportFragmentManager(), null);
                return false;
            }
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermission(final String permission, final int requestCode) {
        ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
    }

    @Override
    public void onCancellingPermissionRationale() {
        showToast(this, getString(R.string.requested_permission_not_granted_rationale));
    }

    private float[] getMFCCdata(byte data[])
    {


        //MFCC java library.
        ShortBuffer shortbuff= ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN).asShortBuffer();
        short[] shortData = new short[shortbuff.limit()];
        shortbuff.get(shortData);
        //short[] shortData=shortbuff.array();
        double[] doubleData= doubleMe(shortData);

        //PStest MFCC mfccConvert = new MFCC();
        MFCCnew mfccConvert = new MFCCnew();
        mfccConvert.setSampleRate((int) 16000);
        int nMFCC = 13; //number of frame
        mfccConvert.setN_mfcc(nMFCC);

        float[] mfccRs = mfccConvert.process(doubleData);
        /*
        int nFFT = mfccRs.length / nMFCC; //frame length
        double[][] mfccValues = new double[nMFCC][nFFT]; //input data of MFCC

        //loop to convert the mfcc values into multi-dimensional array
        for (int i= 0; i<nFFT;i++) {
            int indexCounter = i * nMFCC;
            int rowIndexValue = i % nFFT;
            for (int j= 0;j< nMFCC;j++) {
            mfccValues[j][rowIndexValue] = mfccRs[indexCounter];
            indexCounter++;
            }
        }

        //code to take the mean of mfcc values across the rows such that
        //[nMFCC x nFFT] matrix would be converted into
        //[nMFCC x 1] dimension - which would act as an input to tflite model
        Float[] meanMFCCValues = new Float[nMFCC];
        for (int p=0;p< nMFCC;p++) {
            Double fftValAcrossRow = 0.0;
            for (int q= 0;q<nFFT;q++) {
                fftValAcrossRow = fftValAcrossRow + mfccValues[p][q];
            }
            Double fftMeanValAcrossRow = fftValAcrossRow / nFFT;
            meanMFCCValues[p] = fftMeanValAcrossRow.floatValue();
        }
        return meanMFCCValues;*/
        return mfccRs;

    }

    public static double[] doubleMe(short[] pcms) {
        double[] db = new double[pcms.length];
        for (int i = 0; i < pcms.length; i++) {
            db[i] = (double)pcms[i];
        }
        return db;
    }



    private String processSoundData(byte[] data, String Mac)
    {
        String retStr;
        final int mNumberOfFFTPoints = data.length; //  it should be power of 2
        double mMaxFFTSample;


        double temp;
        Complex[] y;
        Complex[] complexSignal = new Complex[mNumberOfFFTPoints/2];
        double[] absSignal = new double[mNumberOfFFTPoints / 2];


        for (int i = 0; i < mNumberOfFFTPoints / 2; i++) {
            temp = (double) ((data[2 * i] & 0xFF) | (data[2 * i + 1] << 8)) / 32768.0F;
            complexSignal[i] = new Complex(temp, 0.0);
        }

        //                    Compute FFT

        y = FFT1.fft(complexSignal); // --> Here I use FFT class

        mMaxFFTSample = 0.0;
        int mPeakPos = 0;
        for (int i = 0; i < (mNumberOfFFTPoints / 4); i++) {
            absSignal[i] = Math.sqrt(Math.pow(y[i].re(), 2) + Math.pow(y[i].im(), 2));
            if (absSignal[i] > mMaxFFTSample) {
                mMaxFFTSample = absSignal[i];
                mPeakPos = i;
            }
        }
        int peakfreq=mPeakPos*THINGY_MICROPHONE_SAMPLING_FREQ/(mNumberOfFFTPoints/2);
        String LEDcolor;
        if((peakfreq<100)||(mMaxFFTSample<5))
        {//small signal power or low power
            LEDcolor="unchanged";
        }
        else if((peakfreq>=100)&&(peakfreq<500))
        {
            LEDcolor="yellow";
        }
        else if((peakfreq>=500)&&(peakfreq<1000))
        {
            LEDcolor="orange";
        }
        else if ((peakfreq>=1000)&&(peakfreq<2000))
        {
            LEDcolor="red";
        }
        else if ((peakfreq>=2000)&&(peakfreq<3000))
        {
            LEDcolor="green";
        }
        else if ((peakfreq>=3000)&&(peakfreq<4000))
        {
            LEDcolor="blue";
        }
        else if ((peakfreq>=4000)&&(peakfreq<5000))
        {
            LEDcolor="cyan";
        }
        else
        {
            LEDcolor="purple";
        }
        //String for display on textbox mSoundEventLog
        String verbal="Verbal: Peak freq:"+peakfreq+", power:"+mMaxFFTSample;

        //String for control LED
        String ctrLED="LED color:"+ LEDcolor +" @"+Mac;

        //string for advertising
        String advMAC=Mac.replace(":","");
        int pwrInt= (int) Math.round(mMaxFFTSample);
        String advStr="toSink:"+advMAC+String.format("%04X", peakfreq)+ String.format("%04X", pwrInt);
        Log.i("Test3",advStr +",fre:"+String.format("%04X", peakfreq)+",pwr:"+String.format("%04X", pwrInt));

        retStr=verbal +"\r\n"+ ctrLED +"\r\n" +advStr;
        return retStr;
    }

    /* s must be an even-length string. */
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }


    private String soundClassify(Float[] X,String Mac)
    {
        float[] retString;

         int XROW_SIZE=50;
        int XCOL_SIZE=13;

 /*       Float[][] X_img = new Float[XROW_SIZE][XCOL_SIZE];
        for ( int i = 0; i < XROW_SIZE; i++ ) {
            System.arraycopy(X, (i * XCOL_SIZE), X_img[i], 0, XCOL_SIZE);
        }
        Log.d(LOGTAG,"X_img  = " + Arrays.toString(X_img));*/

        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * 13 * 50 * 1);
        byteBuffer.order(ByteOrder.nativeOrder());
        for (int i=0; i<X.length; i++)
            byteBuffer.putFloat(X[i]); // only one channel
        byteBuffer.rewind();

        byteBuffer.rewind();
        // 4 bytes (float), XROW_SIZE x XROW_SIZE x 1
        // X = 0;
        // 4. Compute the location of X, Y = (lat, lng, floor).
        // Tensorflow lite model
        // Initialize interpreter with GPU delegate
        Model.Options options;
        /*CompatibilityList compatList = new CompatibilityList();

        if(compatList.isDelegateSupportedOnThisDevice()){
            // if the device has a supported GPU, add the GPU delegate
            options =  new Model.Options.Builder().setDevice(Model.Device.GPU).build();
        } else {
            // if the GPU is not supported, run on 4 threads
            options = new Model.Options.Builder().setNumThreads(4).build();
        }*/
        try {
            options = new Model.Options.Builder().setNumThreads(4).build();
            SoundClassification model = SoundClassification.newInstance(getApplicationContext(), options);

            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, XROW_SIZE* XCOL_SIZE}, DataType.FLOAT32);
            inputFeature0.loadBuffer(byteBuffer);

            // Runs model inference and gets result.
            SoundClassification.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();
            retString=outputFeature0.getFloatArray();
            Log.i(LOGTAG,"Output  = " + Arrays.toString(outputFeature0.getFloatArray()));
            // Releases model resources if no longer used.
            model.close();

//            setText("dX="+Math.round(xy.x)+"m, dY="+Math.round(xy.y)+"m\nDistance:"+Math.round(dst[0]) +"m");
            //setText("dX="+Math.round(xy.x)+"m, dY="+Math.round(xy.y)+"m\nPos:"+latLng.latitude +", " + latLng.longitude);


        } catch (IOException e) {
            // TODO Handle the exception
            retString=null;

        }
        String retStr;
        String verbal,verbal1;
        String strLed;
        String advMAC= Mac.replace(":","");
        String advStr;
        if(retString[0]>retString[1])
        {
            verbal1=("no sound, output=");
            verbal=verbal1+ Arrays.toString(retString);
            strLed="LED color:"+"green"+" @"+ Mac;
            advStr=null;
        }
        else
        {
            verbal1=("had clap, output=");
            verbal=verbal1+ Arrays.toString(retString);
            strLed="LED color:"+"red"+" @"+ Mac;
            advStr="toSink:"+advMAC+"01";

        }
        retStr=verbal +"\r\n"+ strLed +"\r\n" +advStr;
        Log.i(LOGTAG,retStr);
        return retStr;


    }

    public byte[] readWav(int step) throws IOException {
        byte[] wavData=null;
        try {
            int idx=0;
            switch (step)
            {
                case 0:
                    idx=R.raw.faucet60;
                    break;
                case 1:
                    idx=R.raw.faucet61;
                    break;
                case 2:
                     idx=R.raw.faucet62;
                    break;
                case 3:
                    idx=R.raw.faucet63;
                    break;
                case 4:
                    idx=R.raw.faucet64;
                    break;
                case 5:
                    idx=R.raw.faucet65;
                    break;
                case 6:
                    idx=R.raw.faucet66;
                    break;
                case 7:
                    idx=R.raw.faucet67;
                    break;
                case 8:
                    idx=R.raw.faucet68;
                    break;
                case 9:
                    idx=R.raw.faucet69;
                    break;
                case 10:
                    idx=R.raw.noise60;
                    break;
                case 11:
                    idx=R.raw.noise61;
                    break;
                case 12:
                    idx=R.raw.noise62;
                    break;
                case 13:
                    idx=R.raw.noise63;
                    break;
                case 14:
                    idx=R.raw.noise64;
                    break;
                case 15:
                    idx=R.raw.noise65;
                    break;
                case 16:
                    idx=R.raw.noise66;
                    break;
                case 17:
                    idx=R.raw.noise67;
                    break;
                case 18:
                    idx=R.raw.noise68;
                    break;
                case 19:
                    idx=R.raw.noise69;
                    break;
            }
            InputStream is = getResources().openRawResource(idx);
            wavData = new byte[is.available()];
            String readBytes = String.format(Locale.US, "read bytes = %d", is.read(wavData));
            Log.d(LOGTAG, readBytes);

            is.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        byte[] retArr=new byte[32000];
        for(int i=45;i<32043;i++)
            retArr[i-45]=wavData[i];
        //Arrays.copyOfRange(wavData,45,32043);
        retArr[31998]=retArr[31997];
        retArr[31999]=retArr[31998];
        return retArr;
    }






}

