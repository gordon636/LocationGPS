package apps.wazzzainvst.locationgps;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

public class MainActivity
        extends AppCompatActivity
        implements Observer {

    //TODO String
    private TextView tv_coordinates, tv_startLocation;

    private ListView listView;
    private ArrayList<String> myList;
    public double currentDistance = 0, totalDistance = 0;
    public Button myButton,resetButton;
    private Location startLocation = null;
    private Location currentLocation = null;
    private Location prev_Location = null;
    private LocationHandler handler = null;
    private double overalVelocity;
    private double pointVelocity;
    public double instantVel;
    private boolean permissions_granted;
    private boolean isRestart = false;
    private String myData;
    private Toast ToastMess;
    private DecimalFormat df = new DecimalFormat("0.00");

    private final static int PERMISSION_REQUEST_CODE = 999;
    private static final int REQUEST_LOCATION = 1;
    private static final double EARTH_RADIUS = 6371;
    private final static String LOGTAG = MainActivity.class.getSimpleName();
    private final static String CLICKED = "clicked";
    private final static String MY_LIST = "MyList";
    private final static String MY_DATA = "MyData";
    private final static String MY_GPS = "MyGPSData";
    private final static String MY_INITIAL = "MyInitial";
    private final static String INIT_LAT = "initLat";
    private final static String INIT_LONG = "initLon";
    private final static String INIT_TIME = "initTime";
    private final static String PREV_LAT = "prev_lat";
    private final static String PREV_LONG = "prev_long";
    private final static String PREV_TIME = "pre_time";
    private final static String IS_START_NULL = "start_null";
    private final static String IS_PREV_NULL = "pre_null";
    private final static String IS_RESTART = "restart_or_not";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tv_coordinates = findViewById(R.id.textViewCurrent);
        tv_startLocation = findViewById(R.id.textViewStart);
        myButton = findViewById(R.id.button);
        myButton.setEnabled(false);


        resetButton = findViewById(R.id.buttonReset);
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
                Intent intent = new Intent(MainActivity.this,MainActivity.class);
                startActivity(intent);
            }
        });

        //initiate the handler
        if (handler == null) {
            this.handler = new LocationHandler(this);
            this.handler.addObserver(this);
        }

        listView = findViewById(R.id.listView);

        myList = new ArrayList<String>();
        myButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                 update();
            }
        });


        // check permissions
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
                    PERMISSION_REQUEST_CODE
            );
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
      //  ToastMess = Toast.makeText(getApplicationContext(),"Waiting for GPS provider",Toast.LENGTH_LONG);
       // ToastMess.show();
    }

    public boolean isPermissions_granted() {
        return permissions_granted;
    }


    //permission granted or not
    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            // we have only asked for FINE LOCATION
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                this.permissions_granted = true;
                Log.i(LOGTAG, "Fine location permission granted.");
            }
            else {
                this.permissions_granted = false;
                Log.i(LOGTAG, "Fine location permission not granted.");
            }
        }

    }


    //on click update for the button
    private void update() {
        final SharedPreferences pref = getApplicationContext().getSharedPreferences("Profile", 0); // 0 - for private mode
        final SharedPreferences.Editor editor = pref.edit();


        editor.putBoolean(CLICKED,true).apply();
        myButton.setEnabled(false);
        myButton.setText(R.string.Upd);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                String myData = "";
                overalVelocity = 0.0;
                if(ToastMess != null){
                    ToastMess.cancel();
                }
                if(MainActivity.this.startLocation == null){
                    //get current location
                    MainActivity.this.startLocation = MainActivity.this.currentLocation;
                    String holder = "Start Location: "+startLocation.getLatitude()+", "+startLocation.getLongitude();
                    tv_startLocation.setText(holder);
                    //System.out.println("START LOCATION WAS RESET "+startLocation.getLatitude() + " , "+startLocation.getLongitude()+"/nVelocity: "+startLocation.getSpeed());

                    currentDistance = 0;
                    totalDistance = 0;
                    overalVelocity = 0;
                    pointVelocity = 0;

                    myData = currentLocation.getLatitude() +","+currentLocation.getLongitude()+","+currentDistance+","+totalDistance +"," +pointVelocity;
                    MainActivity.this.prev_Location = MainActivity.this.startLocation;
                }else{

                    //here to update prev
                    currentDistance = calculateDistance(currentLocation.getLatitude(),currentLocation.getLongitude(),
                            prev_Location.getLatitude(), prev_Location.getLongitude());
                    //currentDistance = currentLocation.distanceTo(prev_Location);
                    totalDistance = calculateDistance(currentLocation.getLatitude(),currentLocation.getLongitude()
                            ,startLocation.getLatitude(),startLocation.getLongitude());
                    //totalDistance = currentLocation.distanceTo(startLocation);
                    pointVelocity = Double.isNaN(currentDistance/(currentLocation.getTime()/1000 - prev_Location.getTime()/1000))
                            ? 0.0: currentDistance/(currentLocation.getTime()/1000 - prev_Location.getTime()/1000); //time in ms
                    overalVelocity = Double.isNaN(totalDistance/(currentLocation.getTime() - startLocation.getTime()))
                            ? 0.0: totalDistance/(currentLocation.getTime()/1000 - startLocation.getTime()/1000);

                    myData = currentLocation.getLatitude() +","+currentLocation.getLongitude()+","+currentDistance+","+totalDistance +"," +pointVelocity + ","+ overalVelocity;
                    System.out.println("TEST cur "+ currentDistance+", "+totalDistance +", " +pointVelocity);

                }

                MainActivity.this.prev_Location = MainActivity.this.currentLocation;
                String holder = "Your Current Location: "+currentLocation.getLatitude() + ", "+currentLocation.getLongitude()+
                        "\nInstantaneous Velocity: "+df.format(Double.valueOf(instantVel))+ " m/s" + "\nOverall Velocity: "+df.format(Double.valueOf(overalVelocity))+ " m/s";
                tv_coordinates.setText(holder);


                if (pref.getBoolean(CLICKED, true)) {

                    myList.add(0,myData);
                    listView.setAdapter(new MyAdapter(MainActivity.this, myList));
                    editor.putBoolean(CLICKED,false).apply();
                    myButton.setEnabled(true);
                }
            }
        });
    }



    @Override
    public void update(final Observable observable,
                       Object o) {

        if (observable instanceof LocationHandler) {
            final Location l = (Location) o;
            final double lat = l.getLatitude();
            final double lon = l.getLongitude();
            final long time = l.getTime();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(MainActivity.this.startLocation == null || isRestart){
                     //   ToastMess = Toast.makeText(getApplicationContext(),"Ready to record",Toast.LENGTH_SHORT);
                     //   ToastMess.show();
                        isRestart = false;
                    }

                    MainActivity.this.currentLocation = new Location("");
                    MainActivity.this.currentLocation.setLatitude(lat);
                    MainActivity.this.currentLocation.setLongitude(lon);
                    MainActivity.this.currentLocation.setTime(time);

                    String holder = "Your Current Location: "+currentLocation.getLatitude() + ", "+currentLocation.getLongitude()+
                            "\nInstantaneous Velocity: "+df.format(Double.valueOf(instantVel))+ " m/s" + "\nOverall Velocity: "+df.format(Double.valueOf(overalVelocity))+ " m/s";
                    tv_coordinates.setText(holder);

                    myButton.setEnabled(true);
                }
            });
        }
    }


    private double calculateDistance(double currentLat, double currentLon,double prevLat, double prevLon ){


        double dLat = Math.toRadians((currentLat - prevLat));
        double dLong = Math.toRadians((currentLon - prevLon));

        currentLat = Math.toRadians(currentLat);
        prevLat = Math.toRadians(prevLat);

        double a = Math.pow(Math.sin(dLat/2.0),2) + Math.cos(prevLat) * Math.cos(currentLat)  * Math.pow(Math.sin(dLong/2.0),2);

        double c = 2 * Math.atan(Math.sqrt(a));

        return (EARTH_RADIUS * c)*1000;
    }




    public class MyAdapter extends BaseAdapter
    {
        private Context context;
        private ArrayList<String> MyArr = new ArrayList<String>();

        public MyAdapter(Context c, ArrayList<String> list)
        {
            // TODO Auto-generated method stub
            this.context = c;
            this.MyArr = list;
        }

        public int getCount() {
            // TODO Auto-generated method stub
            return MyArr.size();
        }

        public Object getItem(int position) {
            // TODO Auto-generated method stub
            return position;
        }

        public long getItemId(int position) {
            // TODO Auto-generated method stub
            return position;
        }
        public View getView(final int position, View convertView, ViewGroup parent) {
            // TODO Auto-generated method stub

            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);


            if (convertView == null) {
                convertView = inflater.inflate(R.layout.list_item, null);
            }

            String myData [] = (MyArr.get(position).split(","));

//            System.out.println("TEST 10"+myData);

            // Coordinates
            TextView txtCoordinates = (TextView) convertView.findViewById(R.id.textView_coordiates);
            String holder = "Location: " +myData[0]+", "+myData[1];
            txtCoordinates.setText(holder);



            // Current Distance from last point
            TextView txtDistance = (TextView) convertView.findViewById(R.id.textView_distance_prev);
            holder = getString(R.string.last_dist)+": " +df.format(Double.valueOf(myData[2])).toString()+ " m";
            txtDistance.setText(holder);

            // Total Distance from start
            TextView txtStartDistance = (TextView) convertView.findViewById(R.id.textView_distance_original);
            holder = getString(R.string.start_dist)+": " +df.format(Double.valueOf(myData[3])).toString() + " m";
            txtStartDistance.setText(holder);

            // Velocity
            TextView txtVelocity = (TextView) convertView.findViewById(R.id.textView_velocity);

          //  txtVelocity.setText("Point Velocity: " + df.format(Double.valueOf(myData[5])).toString()+ " m/s");
            holder = "Point Velocity: " + df.format(Double.valueOf(myData[4])).toString()+ " m/s";
            txtVelocity.setText(holder);


            return convertView;

        }
    }



    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {

        // Save UI state changes to the savedInstanceState.
        // This bundle will be passed to onCreate if the process is
        // killed and restarted.

        savedInstanceState.putStringArrayList(MY_LIST,myList);
        savedInstanceState.putString(MY_DATA,tv_coordinates.getText().toString());
        savedInstanceState.putString(MY_GPS,myData);
        savedInstanceState.putString(MY_INITIAL,tv_startLocation.getText().toString());
        savedInstanceState.putBoolean(IS_START_NULL,startLocation == null);
        savedInstanceState.putBoolean(IS_PREV_NULL,prev_Location == null);
        savedInstanceState.putBoolean(IS_RESTART,!isRestart);

        //initial point
        if(startLocation != null) {
            savedInstanceState.putDouble(INIT_LAT, startLocation.getLatitude());
            savedInstanceState.putDouble(INIT_LONG, startLocation.getLongitude());
            savedInstanceState.putLong(INIT_TIME, startLocation.getTime());
        }

        //previous point
        if(prev_Location != null) {
            savedInstanceState.putDouble(PREV_LAT, prev_Location.getLatitude());
            savedInstanceState.putDouble(PREV_LONG, prev_Location.getLongitude());
            savedInstanceState.putLong(PREV_TIME, prev_Location.getTime());
        }


        // etc.

        super.onSaveInstanceState(savedInstanceState);
    }

//onRestoreInstanceState

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {

        super.onRestoreInstanceState(savedInstanceState);

        // Restore UI state from the savedInstanceState.
        // This bundle has also been passed to onCreate.

        tv_coordinates.setText(savedInstanceState.getString(MY_DATA));
        tv_startLocation.setText(savedInstanceState.getString(MY_INITIAL));
        myData = savedInstanceState.getString(MY_GPS);

        myButton.setEnabled(true);
        if(!savedInstanceState.getBoolean(IS_START_NULL)) {
            startLocation = new Location("");
            startLocation.setLatitude(savedInstanceState.getDouble(INIT_LAT));
            startLocation.setLongitude(savedInstanceState.getDouble(INIT_LONG));
            startLocation.setTime(savedInstanceState.getLong(INIT_TIME));
        }

        if(!savedInstanceState.getBoolean(IS_PREV_NULL)) {
            prev_Location = new Location("");
            prev_Location.setLatitude(savedInstanceState.getDouble(PREV_LAT));
            prev_Location.setLongitude(savedInstanceState.getDouble(PREV_LONG));
            prev_Location.setTime(savedInstanceState.getLong(PREV_TIME));
        }

        this.handler = new LocationHandler(this);
        this.handler.addObserver(this);

        myList = savedInstanceState.getStringArrayList(MY_LIST);
        listView.setAdapter(new MyAdapter(MainActivity.this, myList));
        isRestart = savedInstanceState.getBoolean(IS_RESTART);

    }
}