package com.disastermanagment_vjc.www.disastermanagmentorganization;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

import es.dmoral.toasty.Toasty;

public class Login extends AppCompatActivity {

    //Object declarations
    SignInButton googleSignInButton;
    FirebaseAuth firebaseAuth;

    //Sign In Flag Code
    private static final int RC_SIGN_IN=2;

    GoogleApiClient mGoogleApiClient;
    FirebaseAuth.AuthStateListener mAuthListener;
    private DatabaseReference mRootRef,unitRef,userRef;
    Map<String, String> fireBaseMap;



    //OnStart Function is created when the application is launched
    @Override
    protected void onStart() {
        //Function Objective:Check whether the user has previously logged in during the launch of the application
        super.onStart();

        //Add a listener to check for a google account during app start
        if(isNetworkAvailable()) {
            firebaseAuth.addAuthStateListener(mAuthListener);
        }
        else
        {
            Toasty.warning(Login.this,"Please Check Your Internet Connection and Try Again", Toast.LENGTH_SHORT).show();
        }
    }


    //OnCreate is created when the activity is launched
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Function Objective: Configure the Google Sign In and Firebase options during the activity start
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);


        if (isNetworkAvailable()) {
            //Assign the buttons and text in the layout to an Object in the program
            googleSignInButton = findViewById(R.id.googleSignInButton);

            //Initialize firebase for this project
            FirebaseApp.initializeApp(this);

            //Check if a user has previously logged in using their google account
            checkIfUserHasAlreadyLoggedIn();


            //OnClick listener of the google sign in button
            googleSignInButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    signIn();
                }
            });

            // Configure Google Sign In
           configureGoogleSignIn();
        }
        else
        {
            Toasty.warning(Login.this,"Please Check Your Internet Connection and Try Again",Toast.LENGTH_SHORT).show();
        }
    }

    private void checkIfUserHasAlreadyLoggedIn()
    {
        //Function Objective: Check if a user has logged in with their google account previously
        //Get the firebase instance
        firebaseAuth = FirebaseAuth.getInstance();

        //Check the authentication in firebase
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                //If there exists a signed in user
                if (firebaseAuth.getCurrentUser() != null) {
                    //User has already logged in,Jump to Main activity
                    FirebaseUser user = firebaseAuth.getCurrentUser();

                    //Assign user details to LocalDB

                    assignDetailsToLocalDB(user);

                    //Get details from firebase like unitType which is not present in Google Account
                    readDataFromFirebase();

                    //Jump to Main activity
                   jumpToMainActivity();
                }
            }
        };
    }

    private void configureGoogleSignIn()
    {
        //Function Objective:Configure the Google Sign In process
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        // Build a GoogleApiClient with the options specified by gso.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                        Toasty.error(Login.this, "Please Check Your Internet Connection and Try Again", Toast.LENGTH_SHORT).show();
                    }
                })
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();
    }

    private void assignDetailsToLocalDB(FirebaseUser user)
    {
        //Function Objective:Assign the Google Account details to the LocalDB class
        LocalDB.setFullName(user.getDisplayName());
        LocalDB.setEmailAddress(user.getEmail());
        LocalDB.setPhoneNumber(user.getPhoneNumber());
        LocalDB.setProfilePicUri(user.getPhotoUrl());
    }

    private void readDataFromFirebase()
    {
        //Function Objective:Reads the data like unitType from firebase
        getDataBaseReference();

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                //Map that stores retrived data from the DataSnapshot
                if(dataSnapshot!=null) {
                    fireBaseMap = (HashMap<String, String>) dataSnapshot.getValue();
                }


                if(fireBaseMap!=null) {
                    //Retrive data from the snapshot map
                    for (String key : fireBaseMap.keySet()) {

                        switch (key)
                        {
                            case "unitType":
                                LocalDB.setUnitType(fireBaseMap.get(key));
                                break;
                        }

                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    private void jumpToMainActivity()
    {
        //Function Objective:Move to Main Activity
        //Intents are used to jump from one activity/application to another
        Intent intent = new Intent(Login.this, MainActivity.class);
        startActivity(intent);
        finish();
    }


    private void signIn() {
        //Function Objective:The Google Sign In UI is called by this function
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    //Result handler of the sign in function
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        //Function Objective: Result handler of the sign in function
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if(result.isSuccess())
            {
                //Sign into google account
                GoogleSignInAccount account=result.getSignInAccount();
                firebaseAuthWithGoogle(account);
            }
            else
            {
                Toasty.error(this, "Please Check Your Internet Connection and Try Again", Toast.LENGTH_SHORT).show();
            }
        }
    }

    //Link the google account to firebase
    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        //Function Objective:Get the google account credentials like email and password to log in separately to Firebase
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);

        //Sign into firebase with the recieved credentials
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information;
                            FirebaseUser user = firebaseAuth.getCurrentUser();


                            Toasty.success(Login.this, "Success", Toast.LENGTH_SHORT).show();

                            //Retrieve google account data and store it in LocalDB
                            assert user != null;

                            assignDetailsToLocalDB(user);


                            //Move to MainActivity
                            jumpToMainActivity();
                        }
                        else {
                            // If sign in fails, display a message to the user.
                            Toasty.error(Login.this, "Please Check Your Internet Connection and Try Again",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }


    private boolean isNetworkAvailable() {
        //Function Objective: check whether Internet is available
        //Initalize the Connectivity Manager Object
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        assert connectivityManager != null;

        //Connectivity Manager knows whether the phone is connected to Internet or not
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void getDataBaseReference()
    {
        //Function Objective:Create a reference to access the firebase database
        mRootRef = FirebaseDatabase.getInstance().getReference();
        unitRef = mRootRef.child("Units");
        userRef=unitRef.child(LocalDB.getFullName());
    }

}
