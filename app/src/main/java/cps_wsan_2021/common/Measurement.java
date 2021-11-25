package cps_wsan_2021.common;


public class Measurement {

    private int id; // id for database
    private String positionID;
    private String ThingyMAC; // id from the position class, which is the unique name of the position
    private String ThingyName;
    private int SoundValue;
    private int rssi;
    private String timestamp;

    public Measurement() {
    }

    public Measurement(int id, String positionID) {
        this.id = id;
        this.positionID = positionID;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPositionID() {
        return positionID;
    }

    public void setPositionID(String positionID) {
        this.positionID = positionID;
    }

    public String getThingyMAC() {
        return ThingyMAC;
    }

    public void setThingyMAC(String MAC) {
        ThingyMAC = MAC;
    }

    public String getThingyName() {
        return ThingyName;
    }

    public void setThingyName(String name) {
        ThingyName = name;
    }

    public int getSoundValue() {
        return SoundValue;
    }
    public void setSoundValue(int val) {
        this.SoundValue = val;
    }


    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

}