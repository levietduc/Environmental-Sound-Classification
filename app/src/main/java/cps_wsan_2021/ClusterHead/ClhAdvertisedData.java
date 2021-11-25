package cps_wsan_2021.ClusterHead;

import android.util.SparseArray;

import java.util.Arrays;

public class ClhAdvertisedData   {
    //max 18 byte
    //6 bytes for clusterhead communication
    //byte 0: bit 7: =1: routing, =0:data
    //        bit 6..0: source cluster head ID
    public static final int SOURCE_CLH_ID_POS=0;
    public static final int PACKET_CLH_ID_POS=1;
    public static final int DEST_CLH_ID_POS=2;
    public static final int NEXT_HOP_ID_POS=3;
    public static final int HOP_COUNT_POS=4;
    public static final int CONFIGURATION_POS=5;

    //below here, 12 bytes from byte 6 to 17 is user data
    public static final int CLH_USER_DATA_POS= 6;
    public static final int CLH_USER_DATA_CAP=12;

    public static final byte CLH_SET_ROUTING_DATA_MASK=(byte)0x80;
    public static final byte CLH_SET_ROUTING_MODE_SHIFTBIT=(byte)7;
    public static final byte CLH_REQUEST_COMMAND_SHIFTBIT=(byte)6;

    public static final byte CLH_SET_ROUTING_MODE_MASK=(byte)(1<<CLH_SET_ROUTING_MODE_SHIFTBIT);
    public static final byte CLH_REQUEST_COMMAND_MASK=(byte)(1<<CLH_REQUEST_COMMAND_SHIFTBIT);

    public static final byte CLH_COMMAND_SET_SINK=(byte) (0x01<<CLH_REQUEST_COMMAND_SHIFTBIT);


    byte[] ClhAdvData=new byte[16];
    public boolean mRoutingType;
    public byte mSourceID;
    public byte mPacketID;
    public byte mDestinationID;
    public byte mnextHopID;
    public byte mHopCount;
    public int mUniquePacketID; //combination of 2 first bytes of the packet (source ID +packet ID)
    public byte mConfiguration;
    public ClhAdvertisedData(){

    }
    public ClhAdvertisedData (ClhAdvertisedData oldObj)
    {

        ClhAdvData= Arrays.copyOf(oldObj.ClhAdvData,oldObj.ClhAdvData.length);
        mSourceID=oldObj.mSourceID;
        mRoutingType=oldObj.mRoutingType;
        mSourceID=oldObj.mSourceID;
        mPacketID=oldObj.mPacketID;
        mDestinationID=oldObj.mDestinationID;
        mnextHopID=oldObj.mnextHopID;
        mHopCount=oldObj.mHopCount;
        mUniquePacketID=oldObj.mUniquePacketID; //combination of 2 first bytes of the packet (source ID +packet ID)
        mConfiguration=oldObj.mConfiguration;
    }

 /*   public  void Copy(ClhAdvertisedData newObj) {
        byte[] clhdata=newObj.getParcelClhData();
        ClhAdvData=Arrays.copyOf( clhdata, clhdata.length);
    }*/

    public byte[] parcelAdvData(SparseArray<byte[]> manufacturerData, int index)
    {
        mUniquePacketID=manufacturerData.keyAt(index);
        ClhAdvData[SOURCE_CLH_ID_POS]=(byte)(mUniquePacketID>>8);
        ClhAdvData[PACKET_CLH_ID_POS]=(byte)(mUniquePacketID&0x00FF);
        if (manufacturerData.valueAt(index)!=null) {
            System.arraycopy(manufacturerData.valueAt(index), 0,ClhAdvData,PACKET_CLH_ID_POS + 1, manufacturerData.valueAt(index).length);
        }

        mSourceID=(byte)(ClhAdvData[SOURCE_CLH_ID_POS] & 0x007f);
        mPacketID=ClhAdvData[PACKET_CLH_ID_POS];
        mDestinationID=ClhAdvData[DEST_CLH_ID_POS];
        mnextHopID=ClhAdvData[NEXT_HOP_ID_POS];
        mHopCount=ClhAdvData[HOP_COUNT_POS];
        mConfiguration=ClhAdvData[CONFIGURATION_POS];
        mRoutingType=((ClhAdvData[CONFIGURATION_POS] & CLH_SET_ROUTING_MODE_MASK)!=0)?true: false;
        return ClhAdvData;

    }


    public void setSourceID(byte sourceID)
    {
        mSourceID= ClhAdvData[SOURCE_CLH_ID_POS]= (byte) (sourceID & 0x007F);
    }
    public void setPacketID(byte packetID)
    {
        mPacketID= ClhAdvData[PACKET_CLH_ID_POS]=packetID;
    }
    public void setDestId(byte destID)
    {
        mDestinationID=ClhAdvData[DEST_CLH_ID_POS]= (byte) (destID&0x7F);
    }
    public void setNextHopId(byte nextHopId)
    {
        mnextHopID=ClhAdvData[NEXT_HOP_ID_POS]= (byte) (nextHopId&0x7F);

    }


    public void setHopCount(byte hop)
    {
        mHopCount=ClhAdvData[HOP_COUNT_POS]=hop;
    }


    public boolean setUserData(byte[] data)
    {
        if (data.length>CLH_USER_DATA_CAP) return false;
        System.arraycopy(data, 0, ClhAdvData, CLH_USER_DATA_POS, data.length);
        return true;
    }

    public void setConfiguration(byte conf){
        ClhAdvData[CONFIGURATION_POS] = (mConfiguration=conf );
    }


    public byte[] getParcelClhData()
    {
        return ClhAdvData;
    }

    public int getSourcePacketID()
    {
        return (ClhAdvData[SOURCE_CLH_ID_POS]<<8)+((int)(ClhAdvData[PACKET_CLH_ID_POS])&0x00FF);
    }

    public byte getSourceID()
    {
        return ClhAdvData[SOURCE_CLH_ID_POS];
    }
    public byte getPacketID()
    {
        return ClhAdvData[PACKET_CLH_ID_POS];
    }
    public byte getNextHoptID()
    {
        return ClhAdvData[NEXT_HOP_ID_POS];
    }
    public byte getDestinationID()
    {
        return ClhAdvData[DEST_CLH_ID_POS];
    }
    public byte getHopCounts()
    {
        return ClhAdvData[HOP_COUNT_POS];
    }
    public byte getConfiguration()
    {
        return ClhAdvData[CONFIGURATION_POS];
    }

    public boolean isSinkConfig()
    {
        boolean ret=false;

        if(mRoutingType) {
            if ((mConfiguration & CLH_REQUEST_COMMAND_MASK) == CLH_COMMAND_SET_SINK)
                 ret= true;

        }
        return ret;
    }



}
