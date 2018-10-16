package apps.wazzzainvst.locationgps;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;

public class MainActivity
        extends AppCompatActivity
        implements Observer {

    private TextView tv_coordinates;

    private ListView listView;
    private ArrayList myList;
    public double iniLat = 181, iniLon = 181, currentLat, currentLon, prevLat = 181, prevLon = 181, currentDistance = 0, totalDistance = 0;
    private static final double EARTH_RADIUS = 6371;
    public Button myButton;
    private static final int REQUEST_LOCATION = 1;
    private Location startLocation;

    private Observable location;
    private LocationHandler handler = null;
    private final static int PERMISSION_REQUEST_CODE = 999;
    LocationManager locationManager;

    private boolean permissions_granted;
    private final static String LOGTAG =
            MainActivity.class.getSimpleName();
    private LocationManager lm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //  this.tv_lat = findViewById(R.id.tv_lat);
        // this.tv_lon = findViewById(R.id.tv_lon);

        tv_coordinates = findViewById(R.id.textViewCurrent);
        myButton = findViewById(R.id.button);

        startLocation  = new Location("");
        startLocation.setLongitude(0);
        startLocation.setLatitude(0);

        if (handler == null) {
            this.handler = new LocationHandler(this,myButton,tv_coordinates,startLocation);
            this.handler.addObserver(this);
        //    handler.deleteObservers();
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

        myButton.setText("Updating");
        editor.putBoolean("clicked",true).apply();

        if (handler == null) {
            handler = new LocationHandler(MainActivity.this,myButton,tv_coordinates,startLocation);
            handler.addObserver(MainActivity.this);


            System.out.println("TEST 1");

        }
    }


    @Override
    public void update(final Observable observable,
                       Object o) {

        final SharedPreferences pref = getApplicationContext().getSharedPreferences("Profile", 0); // 0 - for private mode
        final SharedPreferences.Editor editor = pref.edit();

        if (observable instanceof LocationHandler) {
            final Location l = (Location) o;
            final double lat = l.getLatitude();
            final double lon = l.getLongitude();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    System.out.println("TEST 2");

                    if (iniLat == 181 && iniLon == 181){

                        iniLat = lat;

                        iniLon = lon;

                        startLocation.setLatitude(lat);
                        startLocation.setLongitude(lon);
                        System.out.println("UPDATE INITIAL ");
                    }

                    currentLat = lat;
                    currentLon = lon;


                    if (prevLat == 181){
                        currentDistance = 0;
                        totalDistance = 0;

                        System.out.println("Prev Lat "+prevLat+" - "+prevLon);
                    }else{
                        //calculate
                        System.out.println("Prev Lat "+prevLat+" - "+prevLon);

                        currentDistance = calculateDistance(currentLat,currentLon,prevLat,prevLon);
                        totalDistance = calculateDistance(currentLat,currentLon,iniLat,iniLon);
                    }

                    String velocity = "0.0";

                    String myData = currentLat +","+currentLon+","+currentDistance+","+totalDistance +"," +velocity;
                    System.out.println("TEST "+myData);

                    if (pref.getBoolean("clicked",true)) {

                        myList.add(0,myData);
                        listView.setAdapter(new MyAdapter(MainActivity.this, myList));
                        editor.putBoolean("clicked",false).apply();

                        prevLat = currentLat;
                        prevLon = currentLon;
                    }
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
            txtCoordinates.setText(myData[0]+", "+myData[1]);

            // Current Distance from last point
            TextView txtDistance = (TextView) convertView.findViewById(R.id.textView_distance_prev);
            txtDistance.setText(getString(R.string.last_dist)+": " +myData[2]+ " km");

            // Total Distance from start
            TextView txtStartDistance = (TextView) convertView.findViewById(R.id.textView_distance_original);
            txtStartDistance.setText(getString(R.string.start_dist)+": " +myData[3] + " km");

            // Velocity
            TextView txtVelocity = (TextView) convertView.findViewById(R.id.textView_velocity);

            txtVelocity.setText(getString(R.string.avg_velocity)+": " + "0.0");


            return convertView;

        }
    }
}