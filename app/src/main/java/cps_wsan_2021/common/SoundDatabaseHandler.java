package cps_wsan_2021.common;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

public class SoundDatabaseHandler extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "soundData";
    // Table Names
    private static final String TABLE_POSITION = "position_AoA";
    private static final String TABLE_MEASUREMENT = "Raw_Sound";

    private static final String TABLE_XREF_ID= "xPosRef";
    // Reference X ID table
    private static final String KEY_XID = "ThingyID";
    private static final String KEY_REFMAC = "MAC";
    private static final String[] XPOSREF_ID_COLUMNS = {KEY_XID,KEY_REFMAC};
    private static final String CREATE_TABLE_XREF_ID = "CREATE TABLE "
            + TABLE_XREF_ID + "(" + KEY_REFMAC + " TEXT PRIMARY KEY,"
            + KEY_XID + " INTEGER"+ ")";

    // Position table - column names
    private static final String KEY_POSITION_ID = "positionID";
    private static final String KEY_POSITION_NAME = "positionName";
    private static final String KEY_POSITION_TYPE = "positionType";
    private static final String KEY_POSITION_lATITUDE = "latitude";
    private static final String KEY_POSITION_lONGITUDE = "longitude";
    private static final String KEY_POSITION_FLOOR = "positionFloor";
    private static final String KEY_POSITION_BUILDING = "positionBuilding";
    private static final String[] POSITION_COLUMNS = {KEY_POSITION_ID, KEY_POSITION_NAME, KEY_POSITION_TYPE, KEY_POSITION_lATITUDE, KEY_POSITION_lONGITUDE,
            KEY_POSITION_FLOOR, KEY_POSITION_BUILDING};

    // Position table - column names
    private static final String KEY_MEASUREMENT_ID = "measurementID";
    private static final String KEY_TIMESTAMP = "timestamp";
    private static final String KEY_MEASUREMENT_THINGYMAC = "ThingyMAC";
    private static final String KEY_MEASUREMENT_THINGYNAME = "ThingyName";
    private static final String KEY_MEASUREMENT_SOUNDDATA = "SoundData";
    private static final String KEY_MEASUREMENT_RSSI = "RSSI";
    private static final String[] MEASUREMENT_COLUMNS = {KEY_MEASUREMENT_ID, KEY_TIMESTAMP, KEY_MEASUREMENT_THINGYMAC, KEY_MEASUREMENT_THINGYNAME,KEY_MEASUREMENT_SOUNDDATA, KEY_MEASUREMENT_RSSI, KEY_TIMESTAMP};

    private static final String CREATE_TABLE_POSITION = "CREATE TABLE "
            + TABLE_POSITION + "(" + KEY_POSITION_ID + " TEXT PRIMARY KEY,"
            + KEY_POSITION_NAME + " TEXT,"
            + KEY_POSITION_TYPE + " TEXT,"
            + KEY_POSITION_lATITUDE + " REAL,"
            + KEY_POSITION_lONGITUDE + " REAL,"
            + KEY_POSITION_FLOOR + " TEXT,"
            + KEY_POSITION_BUILDING + " TEXT" + ")";


    private static final String CREATE_TABLE_MEASUREMENT = "CREATE TABLE "
            + TABLE_MEASUREMENT + "(" + KEY_MEASUREMENT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_TIMESTAMP + "TEXT,"
            + KEY_MEASUREMENT_ID + " TEXT,"
            + KEY_MEASUREMENT_THINGYMAC + " TEXT,"
            + KEY_MEASUREMENT_THINGYNAME + "TEXT, "
            + KEY_MEASUREMENT_SOUNDDATA + " INTERGER,"
            + KEY_MEASUREMENT_RSSI + " INTEGER" + ")";
    // use AUTOINCREMENT to force inserting a new row with id

    public SoundDatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d("SQLiteDatabaseOnCreate", CREATE_TABLE_POSITION);
        db.execSQL(CREATE_TABLE_POSITION);
        Log.d("SQLiteDatabaseOnCreate", CREATE_TABLE_MEASUREMENT);
        db.execSQL(CREATE_TABLE_MEASUREMENT);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // you can implement here migration process
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_POSITION);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MEASUREMENT);
        onCreate(db);
    }

    ////////////////////////// MEASUREMENT TABLE //////////////////////////////////////////
    // delete one measurement
    public void deleteSound(Measurement measurement) {
        // Get reference to writable DB
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_MEASUREMENT, "measurementID = ?", new String[]{String.valueOf(measurement.getId())});
        db.close();
    }

    // delete measurements of a position
    public void deletePositionMeasurements(Position position) {
        // Get reference to writable DB
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_MEASUREMENT, "positionID = ?", new String[]{String.valueOf(position.getId())});
        db.close();
    }

    // retrieve one measurement from the database given id
    public Measurement getMeasurement(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_MEASUREMENT, // a. table
                MEASUREMENT_COLUMNS, // b. column names
                " measurementID = ?", // c. selections
                new String[]{String.valueOf(id)}, // d. selections args
                null, // e. group by
                null, // f. having
                null, // g. order by
                null); // h. limit

        if (cursor != null){
            cursor.moveToFirst();

        Measurement measurement = new Measurement();

        measurement.setId(Integer.parseInt(cursor.getColumnName(0)));
        measurement.setPositionID(cursor.getString(1));
        measurement.setThingyMAC(cursor.getString(2));
        measurement.setThingyName(cursor.getString(3));

        measurement.setRssi(Integer.parseInt(cursor.getColumnName(6)));
        measurement.setTimestamp(cursor.getString(7));

        return measurement;
        }
        return null;
    }

    // retrieve measurement from one position in the database
    public LinkedHashMap<String, Measurement> getPositionMeasurements(int positionID, int limit) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_MEASUREMENT, // a. table
                MEASUREMENT_COLUMNS, // b. column names
                " positionID = ?", // c. selections
                new String[]{String.valueOf(positionID)}, // d. selections args
                null, // e. group by
                null, // f. having
                null, // g. order by
                String.valueOf(limit)); // h. limit

        LinkedHashMap<String, Measurement> positionMeasurements = new LinkedHashMap<>();
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    final String mTimestamp = cursor.getString(cursor.getColumnCount() - 1); // timestamp is stored at the last column
                    final Measurement measurement = new Measurement();
                    measurement.setId(Integer.parseInt(cursor.getColumnName(0)));
                    measurement.setPositionID(cursor.getString(1));
                    measurement.setTimestamp(cursor.getString(2));
                    measurement.setThingyMAC(cursor.getString(3));
                    measurement.setThingyName(cursor.getString(4));
                    measurement.setSoundValue(Integer.parseInt(cursor.getColumnName(5)));
                    measurement.setRssi(Integer.parseInt(cursor.getColumnName(6)));
                    positionMeasurements.put(mTimestamp, measurement);
                } while (cursor.moveToNext());

            }
            cursor.close();
        }

        return positionMeasurements;
    }

//    // get all measurements from database
//    public List<Measurement> getAllMeasurements() {
//
//        List<Measurement> measurements = new LinkedList<Measurement>();
//        String query = "SELECT  * FROM " + DATABASE_NAME;
//        SQLiteDatabase db = this.getWritableDatabase();
//        Cursor cursor = db.rawQuery(query, null);
//        Measurement measurement = null;
//
//        if (cursor.moveToFirst()) {
//            do {
//                measurement = new Measurement();
////                measurement.setId(Integer.parseInt(cursor.getColumnName(0)));
////                measurement.setName(cursor.getString(1));
////                measurement.setType(Integer.parseInt(cursor.getString(2)));
////                measurement.setConnHandle(Integer.parseInt(cursor.getString(3)));
////                measurement.setClusterID(Integer.parseInt(cursor.getString(4)));
////                measurement.setNodeID(Integer.parseInt(cursor.getString(5)));
////                measurement.setHopcount(Integer.parseInt(cursor.getString(6)));
////                measurement.setTemperature(Integer.parseInt(cursor.getString(7)));
////                measurement.setPressure(Integer.parseInt(cursor.getString(8)));
////                measurement.setHumidity(Integer.parseInt(cursor.getString(9)));
////                measurement.setBtnState(Integer.parseInt(cursor.getString(10)));
////                measurement.setLatitude(Integer.parseInt(cursor.getString(11)));
////                measurement.setLongitude(Integer.parseInt(cursor.getString(12)));
////                measurement.setTimestamp(cursor.getString(13));
//                measurements.add(measurement);
//            } while (cursor.moveToNext());
//        }
//
//        return measurements;
//    }

//    // get the last timestamp from database
//    public String getLasTimestamp() {
//
//        String lastTimestamp = null;
//        String query = "SELECT  * FROM " + DATABASE_NAME;
//        SQLiteDatabase db = this.getWritableDatabase();
//        Cursor cursor = db.rawQuery(query, null);
//
//        if (cursor.moveToLast()) {
//            lastTimestamp = cursor.getString(13);
//        }
//        return lastTimestamp;
//    }

    //COLUMNS = {KEY_ID, KEY_NAME, KEY_TYPE, KEY_CONN_HANDLE, KEY_CLUSTER_ID, KEY_NODE_ID,
    //            KEY_HOPCOUNT, KEY_TEMPERATURE, KEY_PRESSURE, KEY_HUMIDITY, KEY_BUTTON_STATE, KEY_TIMESTAMP};

    // add a measurement
    public void addMeasurement(Measurement measurement) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
//        if (measurement.getId()!= -1){
//            values.put(KEY_ID, measurement.getId());
//        }
        values.put(KEY_POSITION_ID, measurement.getPositionID());
        values.put(KEY_MEASUREMENT_THINGYMAC, measurement.getThingyMAC());
        values.put(KEY_MEASUREMENT_THINGYNAME, measurement.getThingyName());
        values.put(KEY_MEASUREMENT_SOUNDDATA, measurement.getSoundValue());
        values.put(KEY_MEASUREMENT_RSSI, measurement.getRssi());
        values.put(KEY_TIMESTAMP, measurement.getTimestamp());
        // insert
        // id will be automatically created, incrementally
        db.insert(TABLE_MEASUREMENT, null, values);
        db.close();
    }

    // update a measurement
    public int updateMeasurement(Measurement measurement) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_POSITION_ID, measurement.getPositionID());
        values.put(KEY_TIMESTAMP, measurement.getTimestamp());
        values.put(KEY_MEASUREMENT_THINGYMAC, measurement.getThingyMAC());
        values.put(KEY_MEASUREMENT_THINGYNAME, measurement.getThingyName());
        values.put(KEY_MEASUREMENT_SOUNDDATA, measurement.getSoundValue());
        values.put(KEY_MEASUREMENT_RSSI, measurement.getRssi());

        int i = db.update(DATABASE_NAME, // table
                values, // column/value
                "measurementID = ?", // selections
                new String[]{String.valueOf(measurement.getId())});

        db.close();

        return i;
    }

    ////////////////////////// POSITION TABLE //////////////////////////////////////////

    // delete one position
    public void deletePosition(Position position) {
        // Get reference to writable DB
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_POSITION, "positionID = ?", new String[]{String.valueOf(position.getId())});
        db.close();
    }


    // retrieve one position from the database given id
    public Position getPosition(String id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_POSITION, // a. table
                POSITION_COLUMNS, // b. column names
                " positionID = ?", // c. selections
                new String[]{String.valueOf(id)}, // d. selections args
                null, // e. group by
                null, // f. having
                null, // g. order by
                null); // h. limit

//        if (cursor != null){
        if (cursor.moveToFirst()){
//            cursor.moveToFirst();

            Position position = new Position();
            position.setId(cursor.getColumnName(0));
            position.setName(cursor.getString(1));
            position.setType(cursor.getString(2));
            // TODO: debug getString and Double.parseDouble, ensure reading all digits.
            position.setLatitude(Double.parseDouble(cursor.getString(3)));
            position.setLongitude(Double.parseDouble(cursor.getString(4)));
            position.setFloor(cursor.getString(5));
            position.setBuilding(cursor.getString(6));
            return position;
        }
        return null;
    }

    // get all positions of a specific type from database
    public List<Position> getPositions(String type) {

        List<Position> positions = new LinkedList<Position>();

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_POSITION, // a. table
                POSITION_COLUMNS, // b. column names
                " positionType = ?", // c. selections
                new String[]{String.valueOf(type)}, // d. selections args
                null, // e. group by
                null, // f. having
                null, // g. order by
                null); // h. limit


        if (cursor.moveToFirst()) {
            do {
                Position position = new Position();
                position.setId(cursor.getColumnName(0));
                position.setName(cursor.getString(1));
                position.setType(cursor.getString(2));
                position.setLatitude(Double.parseDouble(cursor.getString(3)));
                position.setLongitude(Double.parseDouble(cursor.getString(4)));
                position.setFloor(cursor.getString(5));
                position.setBuilding(cursor.getString(6));
                positions.add(position);
            } while (cursor.moveToNext());
        }

        return positions;
    }

    // get all positions from database
    public List<Position> getAllPositions() {

        List<Position> positions = new LinkedList<Position>();
        String query = "SELECT  * FROM " + TABLE_POSITION;
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(query, null);


        if (cursor.moveToFirst()) {
            do {
                Position position = new Position();
                position.setId(cursor.getColumnName(0));
                position.setName(cursor.getString(1));
                position.setType(cursor.getString(2));
                position.setLatitude(Double.parseDouble(cursor.getString(3)));
                position.setLongitude(Double.parseDouble(cursor.getString(4)));
                position.setFloor(cursor.getString(5));
                position.setBuilding(cursor.getString(6));
                positions.add(position);
            } while (cursor.moveToNext());
        }
        return positions;
    }

    // add a position
    public void addPosition(Position position) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
//        if (measurement.getId()!= -1){
//            values.put(KEY_ID, measurement.getId());
//        }
        values.put(KEY_POSITION_ID, position.getId());
        values.put(KEY_POSITION_NAME, position.getName());
        values.put(KEY_POSITION_TYPE, position.getType());
        values.put(KEY_POSITION_lATITUDE,position.getLatitude());
        values.put(KEY_POSITION_lONGITUDE, position.getLongitude());
        values.put(KEY_POSITION_FLOOR, position.getFloor());
        values.put(KEY_POSITION_BUILDING, position.getBuilding());
        db.insert(TABLE_POSITION, null, values);
        db.close();
    }

    // update a position
    public int updatePosition(Position position) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_POSITION_ID, position.getId());
        values.put(KEY_POSITION_NAME, position.getName());
        values.put(KEY_POSITION_TYPE, position.getType());
        values.put(KEY_POSITION_lATITUDE,position.getLatitude());
        values.put(KEY_POSITION_lONGITUDE, position.getLongitude());
        values.put(KEY_POSITION_FLOOR, position.getFloor());
        values.put(KEY_POSITION_BUILDING, position.getBuilding());

        int i = db.update(TABLE_POSITION, // table
                values, // column/value
                "positionID = ?", // selections
                new String[]{String.valueOf(position.getId())});

        db.close();

        return i;
    }

    /////////////////////////////
    int MYDEBUG=0;
    @RequiresApi(api = Build.VERSION_CODES.N)
    public Float[] getXarray(Timestamp currentTime, long msDuration,Float[][]scaleX,LinkedHashMap<String,Integer> iBeaconRef, int maxiBeacon )
    {
        List<Measurement> measurements = new LinkedList<Measurement>();
        LinkedHashMap<String,Integer> TxMACPwr= new LinkedHashMap<String, Integer>();
        LinkedHashMap<String,Integer> TxMACCount= new LinkedHashMap<String, Integer>();
        LinkedHashMap<String,Float> TxMACAvgPwr= new LinkedHashMap<String, Float>();

        int TxElements=0;
        SQLiteDatabase db = this.getReadableDatabase();
        Timestamp timestampRef = currentTime;
        int pwr=0;

        Cursor cursor = db.query(TABLE_MEASUREMENT, // a. table
                MEASUREMENT_COLUMNS, // b. column names
                null, // c. selections
                null, // d. selections args
                null, // e. group by
                null, // f. having
                null, // g. order by
                null); // h. limit
        if (cursor.moveToLast()){
             if(MYDEBUG!=0){ //this code for debug using last sample in database for the Time reference
                 //must be set to 0 for normal operation
                 String timeRefStr=cursor.getString(7);
                 Log.i("getXarray","timeRefStr:"+timeRefStr);
                 try {
                     SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HH:mm:ss:SSS");
                     timestampRef = new Timestamp(dateFormat.parse(timeRefStr).getTime());
                 } catch(Exception e) { //this generic but you can control another types of exception
                     // look the origin of excption
                 }
             }


            int datacnt=0;
            do {//find data within "msDuration" since reference time
                String timeStr=cursor.getString(7);
                Integer rssiPwr=0;
                Timestamp timestamp=null;
                try {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HH:mm:ss:SSS");
                    timestamp = new Timestamp(dateFormat.parse(timeStr).getTime());
                    Log.i("getXarray","timeStamp: "+ timestamp);
                } catch(Exception e) { //this generic but you can control another types of exception
                    // look the origin of excption
                }
                long diff=timestampRef.getTime()-timestamp.getTime();
                if (diff>msDuration) break;
                datacnt++;
                Log.i("getXarray","diff: "+ diff + ", datacnt:"+datacnt);

                rssiPwr=Integer.parseInt(cursor.getString(6));
                String TxMacString=cursor.getString(4);
                if(TxElements==0) {
                    Log.i("getXarray","1st TxMacString:"+TxMacString + ", Pwr:"+rssiPwr);
                    TxElements++;
                    TxMACPwr.put(TxMacString, rssiPwr);
                    TxMACCount.put(TxMacString, 1);
                }
                else{
                    Integer cnt;
                    //check Mac already added in the list
                    if (TxMACPwr.containsKey(TxMacString)==true) {
                        //already existed

                        cnt = TxMACCount.get(TxMacString) + 1;
                        pwr=TxMACPwr.get(TxMacString) + rssiPwr;
                    } else {//new element

                        TxElements++;
                        pwr = rssiPwr;
                        cnt = 1;
                    }
                    Log.i("getXarray", "power, MAC:" + TxMacString+ ",power:" +pwr +" count:"+cnt);
                    TxMACPwr.put(TxMacString, pwr);
                    TxMACCount.put(TxMacString, cnt);
                }
            }
            while (cursor.moveToPrevious());
            cursor.moveToNext();
        }

        for(String key:TxMACPwr.keySet())
        {
            int cnt=TxMACCount.get(key);
            Float sum= Float.valueOf(TxMACPwr.get(key));
            Float average=sum/cnt;
            TxMACAvgPwr.put(key,average);
        }
        Log.i("getXarray","final map:"+TxMACAvgPwr);

        //get ref order of TxMac
        /*Cursor csXRef= db.query(TABLE_XREF_ID,
                XPOSREF_ID_COLUMNS,
                null, // c. selections
                null, // d. selections args
                null, // e. group by
                null, // f. having
                null, // g. order by
                null); // h. limit
        int xSize=csXRef.getCount();


        csXRef.moveToFirst();
        do{
            XRefId.put(csXRef.getString(1),csXRef.getInt(0));
        }while(csXRef.moveToNext());
*/
        Float[] xRet   =new Float[maxiBeacon];
        //initial all elements in array list to 0db
        for(int i=0;i<maxiBeacon;i++)
        {
            xRet[i]= Float.valueOf(-110);
        }

        Log.i("getXarray","iBeaconRef" + iBeaconRef);


        for(String key:TxMACAvgPwr.keySet())
        {
            Log.i("getXarray","txMACkey" + key);

            //lookup MAC in Ref Table
            if (iBeaconRef.containsKey(key)) {
                int pos=iBeaconRef.get(key);
                xRet[pos]=TxMACAvgPwr.get(key);
                Log.i("getXarray","add pos:"+ pos+"="+xRet[pos]+",key="+key +",index:");

            }
        }

        Log.i("getXarray","return array before scale:"+ Arrays.toString(xRet));

        for(int i=0;i<maxiBeacon;i++)
        {
            xRet[i]-=scaleX[0][i];
            xRet[i]/=scaleX[1][i];
        }

        Log.i("getXarray","return array after scale:"+ Arrays.toString(xRet));

        return xRet;
    }
}
