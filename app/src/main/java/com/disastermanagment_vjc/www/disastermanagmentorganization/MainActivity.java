package com.disastermanagment_vjc.www.disastermanagmentorganization;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import io.nlopez.smartlocation.OnLocationUpdatedListener;
import io.nlopez.smartlocation.SmartLocation;
import io.nlopez.smartlocation.location.config.LocationAccuracy;
import io.nlopez.smartlocation.location.config.LocationParams;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {

    //Object Decelerations
    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationClient;
    private int MY_PERMISSIONS_REQUEST_FINE_LOCATION;
    private double initalLat,initalLng,latitude,longitude;
    private DatabaseReference mRootRef,unitRef,userRef;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Function Objective:Load the layout and set initial things
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */

        @SuppressLint("MissingPermission")
        @Override
        public void onMapReady(GoogleMap googleMap) {
                //Function Objective:Display the Map with the app features
                //Assign the Google Map to an Object so that we can use it in program
                mMap = googleMap;

                //Request the permission to access location from the user
                requestPermissionFromUser();

                //Get the last known location in the user's device
                getInitialLocationOfUser();

                //Update the user position periodically both on map and on firebase
                updateUserLocation();
            }

        private void requestPermissionFromUser(){
                //Function Objective:Request location permission from the user
                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            MY_PERMISSIONS_REQUEST_FINE_LOCATION);
                }
            }


        @SuppressLint("MissingPermission")
        private void getInitialLocationOfUser() {
            //Function Objective:Get initial position of user
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            requestPermissionFromUser();
            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            // Got last known location. In some rare situations this can be null.
                            if (location != null) {
                                // Logic to handle location object
                                initalLat=location.getLatitude();
                                initalLng=location.getLongitude();
                            }

                        }
                    });
        }

        private void updateUserLocation()
        {
            //Function Objective:Update user's location periodically

            long mLocTrackingInterval = 1000 * 3; // Tracking interval set at 3 seconds = 3000 milli seconds
            float trackingDistance = 0;//Minimum Tracking distance
            LocationAccuracy trackingAccuracy = LocationAccuracy.HIGH;//Location accuracy set to high,Higher the accuracy higher will be the battery drain

            //Location parameters
            LocationParams.Builder builder = new LocationParams.Builder()
                    .setAccuracy(trackingAccuracy)
                    .setDistance(trackingDistance)
                    .setInterval(mLocTrackingInterval);



            //Customising the marker
            MarkerOptions markerOptions=new MarkerOptions().position(new LatLng(initalLat,initalLng)).title(LocalDB.getFullName());
            final Marker marker=mMap.addMarker(markerOptions);

            //SmartLocation Object tracks the position of the user accurately
            SmartLocation.with(this)
                    .location()
                    .continuous()
                    .config(builder.build())
                    .start(new OnLocationUpdatedListener() {
                        @Override
                        public void onLocationUpdated(Location location) {
                            latitude=location.getLatitude();
                            longitude=location.getLongitude();
                            updateFirebaseData(latitude,longitude);
                            //Create a new user coordinate object
                            LatLng userCoordinates=new LatLng(latitude,longitude);
                            marker.setPosition(userCoordinates);
                            mMap.moveCamera(CameraUpdateFactory.newLatLng(userCoordinates));
                        }
                    });
        }

        private void updateFirebaseData(double lat,double lng)
        {
            //Function Objective:Update the coordinates on the firebase real-time database
            try
            {
                getFirebaseReference();
                userRef.child("lat").setValue(lat);
                userRef.child("ln").setValue(lng);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        private void getFirebaseReference()
        {
            //Function Objective:Create the firebase reference
            mRootRef = FirebaseDatabase.getInstance().getReference();
            unitRef = mRootRef.child("Units");
            userRef=unitRef.child(LocalDB.getFullName());
        }
}
