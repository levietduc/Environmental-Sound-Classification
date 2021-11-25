package cps_wsan_2021.common;


public class Position {

    private String id; // name of the node, should be unique
    private String name;
    private String type;
    private double latitude;
    private double longitude;
    private String floor;
    private String building;
    private String timestamp;

    public Position() {
    }

    public Position(String id, String type) {
        this.id = id;
        this.type = type;
//        this.timestamp = new SimpleDateFormat("HH:mm:ss:SSS", Locale.getDefault()).format(new Date());
//        this.timestamp = new SimpleDateFormat("yyyyMMdd_HH:mm:ss:SSS", Locale.getDefault()).format(new Date()); // the moment of data

    }

    public void setBuilding(String building) {
        this.building = building;
    }

    public String getBuilding() {
        return building;
    }

    public void setFloor(String floor) {
        this.floor = floor;
    }

    public String getFloor() {
        return floor;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}