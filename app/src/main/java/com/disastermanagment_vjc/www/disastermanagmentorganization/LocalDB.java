package com.disastermanagment_vjc.www.disastermanagmentorganization;

import android.net.Uri;

import org.apache.commons.lang3.StringUtils;


public class LocalDB {

    static String fullName,emailAddress,phoneNumber,unitType,status;
    static Uri profilePicUri;


    public static void setEmailAddress(String value) {
        value=value.toLowerCase();
        //Firebase does'nt accept special characters
        value= value.replaceAll("[-+.^:,@!#$%&*()_]","");
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

    public static void setStatus(String value) {
       status = value;
    }

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

    public static Uri getProfilePicUri() {
        return profilePicUri;
    }
}
