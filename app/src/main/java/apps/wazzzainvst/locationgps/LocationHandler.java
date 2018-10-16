package apps.wazzzainvst.locationgps;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import java.util.Observable;

public class LocationHandler
        extends Observable
        implements LocationListener {

    // handles location services for the device
    private LocationManager lm;
    private MainActivity act;
    private Button myButton;
    private float speed;
    private Location l;
    private TextView myLocation;
   Location startLocation ;

    public LocationHandler(MainActivity act, Button aButton, TextView myLocation, Location startLocation) {
        myButton = act.myButton;
        this.myLocation = myLocation;
        this.startLocation = startLocation;
        this.act = act;
        this.lm = (LocationManager) this.act.getSystemService(
                Context.LOCATION_SERVICE);

        if (this.act.checkSelfPermission(
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {


            lm.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    5000,
                    0,
                    this);
            lm.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    5000,
                    0,
                    this);
            lm.requestLocationUpdates(
                    LocationManager.PASSIVE_PROVIDER,
                    5000,
                    0,
                    this);


            // check for initial GPS coordinate
            l = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);




            if (l != null) {
                setChanged();
                notifyObservers(l);
                return;
            }

            l = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (l != null) {

                setChanged();
                notifyObservers(l);
                return;
            }

            l = lm.getLastKnownLocation(
                    LocationManager.PASSIVE_PROVIDER);
            if (l != null) {

                setChanged();
                notifyObservers(l);
                return;
            }

        }
    }

    @Override
    public void onLocationChanged(Location location) {
        setChanged();
        notifyObservers(location);
     //   deleteObservers();

        myButton.setText("Update");
        act.currentLat = location.getLatitude();
        act.currentLon = location.getLongitude();

        myLocation.setText("Your Current Location: "+location.getLatitude() + ", "+location.getLongitude()+ " - Speed: "+location.getSpeed() +  " - " +location.distanceTo(startLocation));

    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }
}