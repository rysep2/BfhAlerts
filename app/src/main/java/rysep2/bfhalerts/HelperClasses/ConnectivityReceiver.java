package rysep2.bfhalerts.HelperClasses;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

/**
 * Created by Pascal on 03/03/2017.
 * Speichern der Anwesenheit für Versionen vor Android 7 Nougat.
 */

public class ConnectivityReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        NetworkConnectivityHelper.handleAvailability(context, Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID), "");
    }
}
