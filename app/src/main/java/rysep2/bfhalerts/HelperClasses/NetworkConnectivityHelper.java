package rysep2.bfhalerts.HelperClasses;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import rysep2.bfhalerts.R;

/**
 * Created by Pascal on 15/04/2017.
 */

public class NetworkConnectivityHelper {

    private static final String TAG = NetworkConnectivityHelper.class.getSimpleName();

    public static void handleAvailability(Context context, String androidId, String helperType) {

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();

        Log.e(TAG, "Connectivity change received");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        if (info != null) {
            if (info.getType() == ConnectivityManager.TYPE_WIFI) {
                // Mit Wifi verbunden
                if (info.isConnected()) {
                    if (prefs.getBoolean("swiDoctor", false) || helperType == "doctors")
                        registerAvailability(context.getResources().getString(R.string.firebaseDoctors), androidId);
                    if (prefs.getBoolean("swiFirefighter", false) || helperType == "firefighters")
                        registerAvailability(context.getResources().getString(R.string.firebaseFirefighters), androidId);
                    if (prefs.getBoolean("swiJanitors", false) || helperType == "janitors")
                        registerAvailability(context.getResources().getString(R.string.firebaseJanitors), androidId);
                    Log.d(TAG, "Verbunden über Wifi");

                } else {

                    unregisterAvailability(context.getResources().getString(R.string.firebaseDoctors), androidId);
                    unregisterAvailability(context.getResources().getString(R.string.firebaseFirefighters), androidId);
                    unregisterAvailability(context.getResources().getString(R.string.firebaseJanitors), androidId);
                    Log.d(TAG, "Nicht verbunden über Wifi");

                }
            } else {

                unregisterAvailability(context.getResources().getString(R.string.firebaseDoctors), androidId);
                unregisterAvailability(context.getResources().getString(R.string.firebaseFirefighters), androidId);
                unregisterAvailability(context.getResources().getString(R.string.firebaseJanitors), androidId);
                Log.d(TAG, "Nicht verbunden über Wifi");
            }
        } else {

            unregisterAvailability(context.getResources().getString(R.string.firebaseDoctors), androidId);
            unregisterAvailability(context.getResources().getString(R.string.firebaseFirefighters), androidId);
            unregisterAvailability(context.getResources().getString(R.string.firebaseJanitors), androidId);
            Log.d(TAG, "Nicht verbunden über Wifi");
        }
    }

    public static void registerAvailability(String helper, String androidId) {
        DatabaseReference mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
        mFirebaseDatabaseReference.child("availabilities").child(helper).child(androidId).setValue("true");
    }

    public static void unregisterAvailability(String helper, String androidId) {
        DatabaseReference mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
        mFirebaseDatabaseReference.child("availabilities").child(helper).child(androidId).removeValue();
    }
}
