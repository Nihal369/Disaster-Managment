package com.disastermanagment_vjc.www.disastermanagmentorganization;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
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

import es.dmoral.toasty.Toasty;

public class Login extends AppCompatActivity {

    //Object declarations
    SignInButton googleSignInButton;
    FirebaseAuth firebaseAuth;
    //Sign In Flag Code
    private static final int RC_SIGN_IN=2;
    GoogleApiClient mGoogleApiClient;
    FirebaseAuth.AuthStateListener mAuthListener;

    @Override
    protected void onStart() {
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



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        if (isNetworkAvailable()) {
            //Decelerations
            googleSignInButton = findViewById(R.id.googleSignInButton);

            TextView textView = (TextView) googleSignInButton.getChildAt(0);
            textView.setTextColor(Color.BLACK);
            textView.setText("Sign In With Google");

            FirebaseApp.initializeApp(this);

            //Check if a user has previously logged in using their google account
            firebaseAuth = FirebaseAuth.getInstance();
            mAuthListener = new FirebaseAuth.AuthStateListener() {
                @Override
                public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                    //If there exists a signed in user
                    if (firebaseAuth.getCurrentUser() != null) {
                        //User has already logged in,Jump to Appointment activity
                        FirebaseUser user = firebaseAuth.getCurrentUser();

                        //Assign user details to LocalDB
                        LocalDB.setFullName(user.getDisplayName());
                        LocalDB.setEmailAddress(user.getEmail());
                        LocalDB.setPhoneNumber(user.getPhoneNumber());
                        LocalDB.setProfilePicUri(user.getPhotoUrl());

                        //Jump to appointment activity
                        Intent intent = new Intent(Login.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    }
                }
            };


            //OnClick listener of the google sign in button
            googleSignInButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    signIn();
                }
            });

            // Configure Google Sign In
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
        else
        {
            Toasty.warning(Login.this,"Please Check Your Internet Connection and Try Again",Toast.LENGTH_SHORT).show();
        }
    }


    //SignIn Function
    private void signIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    //Result handler of the sign in function
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
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
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
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
                            LocalDB.setFullName(user.getDisplayName());
                            LocalDB.setEmailAddress(user.getEmail());
                            LocalDB.setPhoneNumber(user.getPhoneNumber());
                            LocalDB.setProfilePicUri(user.getPhotoUrl());


                            //Continue user registration
                            Intent menuIntent = new Intent(Login.this, MainActivity.class);
                            startActivity(menuIntent);
                            finish();
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
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        assert connectivityManager != null;
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

}
