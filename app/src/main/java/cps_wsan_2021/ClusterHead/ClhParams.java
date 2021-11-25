package cps_wsan_2021.ClusterHead;

public class ClhParams {
    public static final int CLH_TX_POWER_ULTRA_LOW=0;
    public static final int CLH_TX_POWER_LOW=1;
    public static final int CLH_TX_POWER_MEDIUM=2;
    public static final int CLH_TX_POWER_HIGH=3;
    public static final boolean TransmitTxPower=false;


    public byte TxPower=(byte)CLH_TX_POWER_HIGH;
    public boolean isSink=false;
    public byte ClhID=ClhConst.DEFAULT_CLUSTER_HEAD_ID;
    public long advertisingInterval=ClhConst.ADVERTISING_INTERVAL;
    public static final int minClhRSSIThreshold=-100;
    public String ClhName=ClhConst.clusterHeadName;


}


