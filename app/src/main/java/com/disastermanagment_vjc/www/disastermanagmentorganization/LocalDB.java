package com.disastermanagment_vjc.www.disastermanagmentorganization;

import android.net.Uri;

import org.apache.commons.lang3.StringUtils;


public class LocalDB {

    //Description:LOCAL DB is a database class that stores the value retrieved from firebase

    //Object decelerations
    static String fullName,emailAddress,phoneNumber,unitType,status;
    static Uri profilePicUri;

    //Set value functions

    public static void setEmailAddress(String value) {
        //Converting to lower case to maintain a standard
        value=value.toLowerCase();
        //Firebase does'nt accept special characters,Below code removes any special characters
        value= value.replaceAll("[-+.^:,@!#$%&*()_]","");
        emailAddress=value;
    }

    public static void setFullName(String value) {
        //Name of the user is converted into upper case to maintain the standard
        value=value.toUpperCase();
        fullName = value;
    }

    public static void setPhoneNumber(String value) {phoneNumber = value;}

    public static void setProfilePicUri(Uri value) {profilePicUri = value;}

    public static void setUnitType(String value) {
        unitType = value;
    }

    public static void setStatus(String value) {
       status = value;
    }

    //Get value functions

    public static String getStatus() {
        return status;
    }

    public static String getUnitType() {
        return unitType;
    }

    public static String getEmailAddress() {
        return emailAddress;
    }

    public static String getFullName() {
        return fullName;
    }

    public static String getPhoneNumber() {
        return phoneNumber;
    }

    public static Uri getProfilePicUri() {return profilePicUri;}

}
