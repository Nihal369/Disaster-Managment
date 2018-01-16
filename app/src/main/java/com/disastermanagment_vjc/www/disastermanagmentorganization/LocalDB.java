package com.disastermanagment_vjc.www.disastermanagmentorganization;

import android.net.Uri;


public class LocalDB {

    static String fullName,emailAddress,phoneNumber,unitType;
    static Uri profilePicUri;


    public static void setEmailAddress(String value) {
        emailAddress=value;
    }

    public static void setFullName(String value) {
        value=value.toUpperCase();
        fullName = value;
    }

    public static void setPhoneNumber(String value) {
        phoneNumber = value;
    }

    public static void setProfilePicUri(Uri value) {
        profilePicUri = value;
    }

    public static void setUnitType(String value) {
        unitType = value;
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

    public static Uri getProfilePicUri() {
        return profilePicUri;
    }
}
