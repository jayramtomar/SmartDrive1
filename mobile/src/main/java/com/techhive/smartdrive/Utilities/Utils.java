package com.techhive.smartdrive.Utilities;

/**
 * Created by Sunain Mittal on 1/1/2018.
 */
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class Utils {

    private Context mContext = null;

    public Utils(Context con) {
        mContext = con;
    }
    public static String encodeEmail(String userEmail) {
        return userEmail.replace(".", ",");
    }
    public boolean isNetworkAvailable() {

        ConnectivityManager connectivityManager

                = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();

        return activeNetworkInfo != null && activeNetworkInfo.isConnected();

    }
}