package cps_wsan_2021.ClusterHead;
/*important constants*/
public class ClhConst {
    public static final int DEFAULT_CLUSTER_HEAD_ID =1;

    //for advertiser
    public static final String clusterHeadName="Clh";
    public static final int MAX_ADVERTISE_LIST_ITEM=512; //max items in waiting list for advertising
    public static final int ADVERTISING_INTERVAL=200; //default 200 ms interval for each advertising packet

    public static final int MAX_ADV_DATA_LENGTH=31; //fix for BLE 4
    //----------
    //for scanner
    public static final int MIN_SCAN_RSSI_THRESHOLD=-80;    //min RSSI of receive packet from other clusterheads
    public static final long SCAN_PERIOD = 60000*5;   //scan 10 minutes
    public static final long REST_PERIOD=1000; //rest in 1 sec
    public static final int SCAN_HISTORY_LIST_SIZE=512*2; //max item in history list

    //for processor
    public static final int MAX_PROCESS_LIST_ITEM=128; //max items in waiting list for processing

}
