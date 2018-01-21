package com.disastermanagment_vjc.www.disastermanagmentorganization;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.shashank.sony.fancydialoglib.Animation;
import com.shashank.sony.fancydialoglib.FancyAlertDialog;
import com.shashank.sony.fancydialoglib.FancyAlertDialogListener;
import com.shashank.sony.fancydialoglib.Icon;

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
    ImageView statusButtonImage;
    Map<String,Circle> fireArea;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Function Objective:Load the layout and set initial things
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestPermissionFromUser();
        usersMarkersMap=new HashMap<>();

        rescuerCard=findViewById(R.id.rescuerCard);
        statusButtonImage=findViewById(R.id.statusImageView);
        if(LocalDB.getStatus().equals("available")) {
            statusButtonImage.setImageResource(R.drawable.available);
        }
        else
        {
            statusButtonImage.setImageResource(R.drawable.busy);
        }

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
        updateFirebaseData(initalLat,initalLng,LocalDB.getUnitType(),LocalDB.getStatus());

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
                        updateFirebaseData(latitude,longitude,LocalDB.getUnitType(),LocalDB.getStatus());
                    }
                });
    }

    private void updateFirebaseData(double latValue,double lngValue,String unitTypeValue,String statusValue)
    {
        //Function Objective:Update the coordinates on the firebase real-time database
        try
        {
            getFirebaseReference();
            userRef.child("lat").setValue(latValue);
            userRef.child("ln").setValue(lngValue);
            assert unitTypeValue!=null;
            userRef.child("unitType").setValue(unitTypeValue);
            userRef.child("status").setValue(statusValue);
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

                                if(keyValuePairs.length==4) {//Check if 4 parameters lat,lng,status and unitType are retrieved
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
                                        BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(getIconPathFromDrawable(subMap.get("unitType"),subMap.get("status")));
                                        marker.setIcon(icon);//Update icon if necessary

                                    } else {
                                        //If the marker for a user doesnt exist,we need to create a new one
                                        //We cant directly give an image as a marker,we need to convert it into a bitmap icon.
                                        BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(getIconPathFromDrawable(subMap.get("unitType"),subMap.get("status")));
                                        Marker usersMarker = mMap.addMarker(new MarkerOptions()
                                                .position(new LatLng(Double.parseDouble(subMap.get("lat")), Double.parseDouble(subMap.get("ln"))))
                                                .title(key)
                                                .icon(icon));

                                        usersMarkersMap.put(key, usersMarker);
                                    }

                                    //Draw fire circle around fire
                                    if(subMap.get("unitType").equals("fire"))
                                    {
                                        drawFireCircle(new LatLng(Double.parseDouble(subMap.get("lat")), Double.parseDouble(subMap.get("ln"))));
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

    private int getIconPathFromDrawable(String unitValue,String statusValue)
    {
        //Function Objective:Return the image path for the corresponding unitType
        if(unitValue==null || statusValue==null)
        {
            return 0;
        }
            switch (unitValue) {
                case "ambulance":
                    if(statusValue.equals("available")) {
                        return R.drawable.ambulanceavailable;
                    }
                    else
                    {
                        return R.drawable.ambulancebusy;
                    }
                case "firefighter":
                    if(statusValue.equals("available")) {
                        return R.drawable.firetruckavailable;
                    }
                    else
                    {
                        return R.drawable.firetruckbusy;
                    }
                case "rescuer":
                    if(statusValue.equals("available")) {
                        return R.drawable.rescueravailable;
                    }
                    else
                    {
                        return R.drawable.rescuerbusy;
                    }
                case "victim":
                    switch (statusValue)
                    {
                        case "deceased":return R.drawable.victimdeceased;
                        case "critical":return R.drawable.victimcritical;
                        case "injured":return R.drawable.victiminjured;
                        case "fine":return R.drawable.victimfine;
                    }
                    break;
                case "fire":return R.drawable.fire;
            }
            return R.drawable.rescueravailable;

    }

    private void implementUnitType()
    {
        //Function Objective:Implement user's unitType functions
        //Example:If the signed in user is a firefighter,implement firefighter properties only
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
        //Function Objective:Implement firefighter properties
        //TODO:COMPLETE FIREFIGHTER MODULE
        //Add a victim by long pressing on the victim's location
        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(final LatLng latLng) {

                //Add fire to the selected location

                new FancyAlertDialog.Builder(MainActivity.this)
                        .setTitle("Add Fire")
                        .setBackgroundColor(Color.parseColor("#F39C12"))  //Don't pass R.color.colorvalue
                        .setMessage("Are you sure you want to add the fire")
                        .setNegativeBtnText("No")
                        .setPositiveBtnBackground(Color.parseColor("#D35400"))  //Don't pass R.color.colorvalue
                        .setPositiveBtnText("Yes")
                        .setNegativeBtnBackground(Color.parseColor("#A9A7A8"))  //Don't pass R.color.colorvalue
                        .setAnimation(Animation.POP)
                        .isCancellable(true)
                        .setIcon(R.drawable.ic_error_outline_white_48dp, Icon.Visible)

                        .OnPositiveClicked(new FancyAlertDialogListener() {
                            @Override
                            public void OnClick() {

                              addFireToFirebase(latLng.latitude,latLng.longitude);

                            }
                        })

                        .OnNegativeClicked(new FancyAlertDialogListener() {
                            @Override
                            public void OnClick() {
                            }
                        })
                        .build();
            }
        });

        //Remove fire
        removeFire();
    }

    private void ambulance()
    {
        //Function Objective:Implement ambulance properties
        attendVictim();
    }

    private void rescuer()
    {
        //Function Objective:Implement rescuer properties

        //Add a victim by long pressing on the victim's location
        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                //Display the layout that gives the victim health options like deceased,critical etc
                rescuerCard.setVisibility(View.VISIBLE);
                victimLatLng=latLng;
            }
        });

        //Attend a victim
        attendVictim();

    }

    private void addFireToFirebase(double latValue,double lngValue)
    {
        //Function Objective:Add a new fire to firebase
        try {
            mRootRef = FirebaseDatabase.getInstance().getReference();
            unitRef = mRootRef.child("Units");
            //Generate a random fire child in the Units ref
            String fireId=generateFireName();
            userRef=unitRef.child(fireId);
            userRef.child("lat").setValue(latValue);
            userRef.child("ln").setValue(lngValue);
            userRef.child("unitType").setValue("fire");
            userRef.child("status").setValue("fire");

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void drawFireCircle(LatLng latLngValue)
    {
        // Instantiates a new CircleOptions object and defines the center and radius
        CircleOptions circleOptions = new CircleOptions()
                .strokeColor(Color.RED) //Outer black border
                .fillColor(Color.argb(64, 255, 0, 0)) //inside of the geofence will be transparent, change to whatever color you prefer like 0x88ff0000 for mid-transparent red
                .center(latLngValue) // the LatLng Object of your geofence location
                .radius(8000); // The radius (in meters) of your geofence

        // Get back the mutable Circle
        Circle circle= mMap.addCircle(circleOptions);
        //return circle;
    }


    private String generateFireName()
    {
        //Generate a random vicitm name like VICTIM 1010,VICTIM 3168 etc
        String victimName="FIRE ";

        //Java Random Number Generator
        Random rand = new Random();
        int  n = rand.nextInt(8999) + 1000;
        //9999 is the maximum and the 1000 is our minimum

        //Add the random number to the String "VICTIM"
        victimName+=n;

        return victimName;
    }


    private void removeFire()
    {

        //Remove a Fire,thereby deleting the marker,This is done by clicking on the Fire marker
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(final Marker marker) {


                if (marker.getTitle().contains("FIRE")) {
                    //Only remove FIRE markers

                    //Give an alert to confirm the removal

                    new FancyAlertDialog.Builder(MainActivity.this)
                            .setTitle("Remove Fire")
                            .setBackgroundColor(Color.parseColor("#F39C12"))  //Don't pass R.color.colorvalue
                            .setMessage("Are you sure you want to extinguish the fire")
                            .setNegativeBtnText("No")
                            .setPositiveBtnBackground(Color.parseColor("#D35400"))  //Don't pass R.color.colorvalue
                            .setPositiveBtnText("Yes")
                            .setNegativeBtnBackground(Color.parseColor("#A9A7A8"))  //Don't pass R.color.colorvalue
                            .setAnimation(Animation.POP)
                            .isCancellable(true)
                            .setIcon(R.drawable.ic_error_outline_white_48dp, Icon.Visible)

                            .OnPositiveClicked(new FancyAlertDialogListener() {
                                @Override
                                public void OnClick() {
                                    //Remove the victims from everywhere
                                    deleteVictimOrFireFromFireBase(marker.getTitle());
                                    usersMarkersMap.remove(marker.getTitle());
                                    marker.remove();

                                }
                            })

                            .OnNegativeClicked(new FancyAlertDialogListener() {
                                @Override
                                public void OnClick() {
                                }
                            })
                            .build();
                }
                return false;
            }
        });
    }

    private void attendVictim()
    {
        //Attend a victim,thereby deleting the marker,This is done by clicking on the victim marker
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(final Marker marker) {


                if (marker.getTitle().contains("VICTIM")) {
                    //Only attend Victim markers

                    //Give an alert to confirm the attend

                    new FancyAlertDialog.Builder(MainActivity.this)
                            .setTitle("Attend the victim")
                            .setBackgroundColor(Color.parseColor("#F44336"))  //Don't pass R.color.colorvalue
                            .setMessage("Are you sure you want to attend the Victim")
                            .setNegativeBtnText("No")
                            .setPositiveBtnBackground(Color.parseColor("#D32F2F"))  //Don't pass R.color.colorvalue
                            .setPositiveBtnText("Yes")
                            .setNegativeBtnBackground(Color.parseColor("#A9A7A8"))  //Don't pass R.color.colorvalue
                            .setAnimation(Animation.POP)
                            .isCancellable(true)
                            .setIcon(R.drawable.ic_error_outline_white_48dp, Icon.Visible)

                            .OnPositiveClicked(new FancyAlertDialogListener() {
                                @Override
                                public void OnClick() {

                                    //Remove the victims from everywhere
                                    deleteVictimOrFireFromFireBase(marker.getTitle());
                                    usersMarkersMap.remove(marker.getTitle());
                                    marker.remove();

                                }
                            })

                            .OnNegativeClicked(new FancyAlertDialogListener() {
                                @Override
                                public void OnClick() {
                                }
                            })
                            .build();
                }
                return false;
            }
        });
    }

    public void setVictim(View view)
    {
        //Function Objective:Create a new victim

        //Below values are obtained when the user long presses on the map
        double latValue,lngValue;
        latValue=victimLatLng.latitude;
        lngValue=victimLatLng.longitude;

        //Menu for which button is selected
        switch (view.getId())
        {
            case R.id.deceasedButton:
                addVictimToFireBase(latValue,lngValue,"victim","deceased");
                break;
            case R.id.criticalButton:
                addVictimToFireBase(latValue,lngValue,"victim","critical");
                break;
            case R.id.injuredButton:
                addVictimToFireBase(latValue,lngValue,"victim","injured");
                break;
            case R.id.fineButton:
                addVictimToFireBase(latValue,lngValue,"victim","fine");
                break;
        }
    }

    private void deleteVictimOrFireFromFireBase(String victimName)
    {
        //Function Objective:Remove the victim record from firebase
        mRootRef = FirebaseDatabase.getInstance().getReference();
        unitRef = mRootRef.child("Units");
        //Delete the entire victim objects in the Units tree
        unitRef.child(victimName).removeValue();
    }


    private void addVictimToFireBase(double latValue,double lngValue,String unitTypeValue,String statusValue)
    {
        //Function Objective:Add a new victim to firebase
        try
        {
            mRootRef = FirebaseDatabase.getInstance().getReference();
            unitRef = mRootRef.child("Units");
            //Generate a random victim child in the Units ref
            userRef=unitRef.child(generateVictimName());
            userRef.child("lat").setValue(latValue);
            userRef.child("ln").setValue(lngValue);
            userRef.child("unitType").setValue(unitTypeValue);
            userRef.child("status").setValue(statusValue);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        rescuerCard.setVisibility(View.INVISIBLE);
    }

    private String generateVictimName()
    {
        //Generate a random vicitm name like VICTIM 1010,VICTIM 3168 etc
        String victimName="VICTIM ";

        //Java Random Number Generator
        Random rand = new Random();
        int  n = rand.nextInt(8999) + 1000;
        //9999 is the maximum and the 1000 is our minimum

        //Add the random number to the String "VICTIM"
        victimName+=n;

        return victimName;
    }

    public void changeStatus(View view)
    {
        //Function Objective:Change status in LocalDB & Firebase
        if(LocalDB.getStatus().equals("available"))
        {
            LocalDB.setStatus("busy");
            statusButtonImage.setImageResource(R.drawable.busy);
        }
        else
        {
            LocalDB.setStatus("available");
            statusButtonImage.setImageResource(R.drawable.available);
        }
        updateFirebaseData(latitude,longitude,LocalDB.getUnitType(),LocalDB.getStatus());
    }

}