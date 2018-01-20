package com.disastermanagment_vjc.www.disastermanagmentorganization;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

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
    Map<String,String> fireBaseMap;
    Map<String,Marker>usersMarkersMap;
    CardView rescuerCard;
    LatLng victimLatLng;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Function Objective:Load the layout and set initial things
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestPermissionFromUser();
        usersMarkersMap=new HashMap<>();

        rescuerCard=findViewById(R.id.rescuerCard);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        //Function Objective:Display the Map with the app features
        //Assign the Google Map to an Object so that we can use it in program
        mMap = googleMap;

        //Request the permission to access location from the user
        requestPermissionFromUser();

        try {
            //Get the last known location in the user's device
            getInitialLocationOfUser();

            //Update the user position periodically both on map and on firebase
            updateUserLocation();

            //Read the database to get information about rest of the units
            readDataFromFirebase();

            //Select the appropriate module to be executed according to unitType
            implementUnitType();

        }
        catch (Exception e)
        {
            requestPermissionFromUser();
        }
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

        //Update the initial position to firebase
        updateFirebaseData(initalLat,initalLng,LocalDB.getUnitType());

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
                        updateFirebaseData(latitude,longitude,LocalDB.getUnitType());
                    }
                });
    }

    private void updateFirebaseData(double latValue,double lngValue,String unitTypeValue)
    {
        //Function Objective:Update the coordinates on the firebase real-time database
        try
        {
            getFirebaseReference();
            userRef.child("lat").setValue(latValue);
            userRef.child("ln").setValue(lngValue);
            assert unitTypeValue!=null;
            userRef.child("unitType").setValue(unitTypeValue);
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

    private void readDataFromFirebase()
    {
        //Function Objective:Reads the data like unitType from firebase
        getFirebaseReference();

            unitRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    //Map that stores retrived data from the DataSnapshot
                    if (dataSnapshot != null) {
                        fireBaseMap = (HashMap<String, String>) dataSnapshot.getValue();
                    }

                    if (fireBaseMap != null) {
                        //firebase map eg{NAME={lat=0,ln=0,unitType=rescuer},NAME2........}

                        //Retrieve data from the snapshot map
                        for (String key : fireBaseMap.keySet()) {

                            if (fireBaseMap.get(key) != null) {

                                //Split the value string into a hashmap
                                //Eg:{lat:0,ln:0,unitType:rescuer} is split into a hashmap
                                String value = String.valueOf(fireBaseMap.get(key));
                                value = value.substring(1, value.length() - 1);           //remove curly brackets
                                String[] keyValuePairs = value.split(",");              //split the string to create key-value pairs
                                Map<String, String> subMap = new HashMap<>();

                                if(keyValuePairs.length==3) {//Check if 3 parameters lat,lng and unitType are retrieved
                                    for (String pair : keyValuePairs)                        //iterate over the pairs
                                    {
                                        String[] entry = pair.split("=");                   //split the pairs to get key and value
                                        subMap.put(entry[0].trim(), entry[1].trim());          //add them to the hashmap and trim whitespaces
                                    }

                                    //UPDATE MARKERS ON MAP
                                    if (usersMarkersMap.containsKey(key)) {
                                        //If the current user's marker existed previously then we only need to update it rather create another marker which leads to 2 markers for a single user
                                        Marker marker = usersMarkersMap.get(key);
                                        marker.setPosition(new LatLng(Double.parseDouble(subMap.get("lat")), Double.parseDouble(subMap.get("ln")))); // Update your marker
                                    } else {
                                        //If the marker for a user doesnt exist,we need to create a new one
                                        //We cant directly give an image as a marker,we need to convert it into a bitmap icon.
                                        BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(getIconPathFromDrawable(subMap.get("unitType")));
                                        Marker usersMarker = mMap.addMarker(new MarkerOptions()
                                                .position(new LatLng(Double.parseDouble(subMap.get("lat")), Double.parseDouble(subMap.get("ln"))))
                                                .title(key)
                                                .icon(icon));

                                        usersMarkersMap.put(key, usersMarker);
                                    }
                                }

                            }
                        }
                    }

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
    }

    private int getIconPathFromDrawable(String value)
    {
        //Function Objective:Return the image path for the corresponding unitType
        if(value==null)
        {
            return 0;
        }
            switch (value) {
                case "ambulance":
                    return R.drawable.ambulance;
                case "firefighter":
                    return R.drawable.firetruck;
                case "rescuer":
                    return R.drawable.rescuer;
                case "victimDeceased":
                    return R.drawable.victimdeceased;
                case "victimCritical":
                    return R.drawable.victimcritical;
                case "victimInjured":
                    return R.drawable.victiminjured;
                case "victimFine":
                    return R.drawable.victimfine;
            }
            return R.drawable.victimfine;

    }

    private void implementUnitType()
    {
        switch (LocalDB.getUnitType())
        {
            case "firefighter":firefighter();
            break;
            case "ambulance":ambulance();
            break;
            case "rescuer":rescuer();
            break;
            default:rescuer();
        }
    }

    private void firefighter()
    {
        //TODO:COMPLETE FIREFIGHTER MODULE
    }

    private void ambulance()
    {
        //TODO:COMPLETE AMBULANCE MODULE
    }

    private void rescuer()
    {
        //TODO:COMPLETE RESCUER MODULE
        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                rescuerCard.setVisibility(View.VISIBLE);
                victimLatLng=latLng;
            }
        });

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                //TODO:BUILD AN ALERT BOX
                deleteVictimFromFireBase(marker.getTitle());
                usersMarkersMap.remove(marker.getTitle());
                marker.remove();
                return false;
            }
        });
    }

    public void setVictim(View view)
    {
        double latValue,lngValue;
        latValue=victimLatLng.latitude;
        lngValue=victimLatLng.longitude;

        switch (view.getId())
        {
            case R.id.deceasedButton:
                addVictimToFireBase(latValue,lngValue,"victimDeceased");
                break;
            case R.id.criticalButton:
                addVictimToFireBase(latValue,lngValue,"victimCritical");
                break;
            case R.id.injuredButton:
                addVictimToFireBase(latValue,lngValue,"victimInjured");
                break;
            case R.id.fineButton:
                addVictimToFireBase(latValue,lngValue,"victimFine");
                break;
        }
    }

    private void deleteVictimFromFireBase(String victimName)
    {
        mRootRef = FirebaseDatabase.getInstance().getReference();
        unitRef = mRootRef.child("Units");
        unitRef.child(victimName).removeValue();
    }



    private void addVictimToFireBase(double latValue,double lngValue,String unitTypeValue)
    {
        //Function Objective:Add a new victim to firebase
        try
        {
            mRootRef = FirebaseDatabase.getInstance().getReference();
            unitRef = mRootRef.child("Units");
            userRef=unitRef.child(generateVictimName());
            userRef.child("lat").setValue(latValue);
            userRef.child("ln").setValue(lngValue);
            userRef.child("unitType").setValue(unitTypeValue);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        rescuerCard.setVisibility(View.INVISIBLE);
    }

    private String generateVictimName()
    {
        String victimName="VICTIM ";
        Random rand = new Random();

        int  n = rand.nextInt(8999) + 1000;
        //9999 is the maximum and the 1000 is our minimum
        victimName+=n;
        return victimName;
    }
}