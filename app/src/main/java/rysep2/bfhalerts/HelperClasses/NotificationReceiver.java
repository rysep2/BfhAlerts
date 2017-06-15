package rysep2.bfhalerts.HelperClasses;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.io.IOException;
import java.util.Map;

import rysep2.bfhalerts.AlertActivity;
import rysep2.bfhalerts.DatabaseObjects.Alert;
import rysep2.bfhalerts.MainActivity;
import rysep2.bfhalerts.R;

import static android.app.Notification.DEFAULT_ALL;

/**
 * Created by Pascal on 23/03/2017.
 */

public class NotificationReceiver extends FirebaseMessagingService {
    final String TAG = NotificationReceiver.class.getSimpleName();
    boolean isAvailable = false;
    String androidId = "";

    Map<String, String> data;

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        data = remoteMessage.getData();
        Log.d(TAG, "Notification empfangen");

        helperIsAvailable();

        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Holen der mitgegebenen Daten
        String alertId = data.get("id");
        String alertName = Alert.convertAlertTypeToName(Integer.parseInt(data.get("alerttype")), NotificationReceiver.this);
        long alertStartTime = Long.parseLong(data.get("starttime"));

        // Builden der Notification
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(NotificationReceiver.this)
                        .setSmallIcon(R.drawable.bfh_alerts_notif)
                        .setContentTitle(alertName)
                        .setContentText(MainActivity.getDate(alertStartTime, "dd.MM.yyyy HH:mm:ss"))
                        .setLights(Color.RED, 500,500)
                        .setDefaults(DEFAULT_ALL);

        int notificationId = 001;

        // Erstellen des zu öffnenden Intents beim Klick auf Notification
        Intent alertIntent = new Intent(NotificationReceiver.this, AlertActivity.class);
        alertIntent.putExtra("alertId", alertId);
        alertIntent.putExtra("alertName", alertName);
        alertIntent.addCategory("ALARM.OHNE_ZUSAGE");
        alertIntent.addFlags(Intent.FLAG_FROM_BACKGROUND);

        // TaskStackBuilder damit beim Klick auf den Zurück-Button wieder auf die MainActivity weitergeleitet wird
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(NotificationReceiver.this);
        stackBuilder.addParentStack(AlertActivity.class);
        stackBuilder.addNextIntent(alertIntent);

        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);
        mBuilder.setAutoCancel(true);

        // Button "Hilfe zusagen" anfügen mit eigenem Stack
        Intent zusageAlertIntent = new Intent(NotificationReceiver.this, AlertActivity.class);
        zusageAlertIntent.putExtra("alertId", alertId);
        zusageAlertIntent.putExtra("alertName", alertName);
        zusageAlertIntent.putExtra("zusage", true);
        zusageAlertIntent.addCategory("ALARM.MIT_ZUSAGE");

        TaskStackBuilder zusageStackBuilder = TaskStackBuilder.create(NotificationReceiver.this);
        zusageStackBuilder.addParentStack(AlertActivity.class);
        zusageStackBuilder.addNextIntent(zusageAlertIntent);

        PendingIntent zusagePendingIntent = zusageStackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.addAction(R.drawable.bfh_alerts_notif, "Hilfe Zusagen", zusagePendingIntent);

        // Holt eine NotificationManager Instanz
        NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (isAvailable) {
            // Notification erstellen und senden
            mNotifyMgr.notify(notificationId, mBuilder.build());

            // Sound-Datei erstellen
            Uri alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            MediaPlayer mMediaPlayer = new MediaPlayer();
            try {
                mMediaPlayer.setDataSource(this, alert);
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Sound abspielen
            final AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0);

            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
            mMediaPlayer.setLooping(false);
            try {
                mMediaPlayer.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mMediaPlayer.start();

        }
    }

    private void helperIsAvailable() {
        DatabaseReference refMain = FirebaseDatabase.getInstance().getReference();

        androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        String helper = "";
        if (prefs.getBoolean("swiDoctor", false))
            helper = getResources().getString(R.string.firebaseDoctors);
        if (prefs.getBoolean("swiFirefighter", false))
            helper = getResources().getString(R.string.firebaseFirefighters);
        if (prefs.getBoolean("swiJanitors", false))
            helper = getResources().getString(R.string.firebaseJanitors);

        if (helper != "") {
            refMain.child("availabilities").child(helper).addListenerForSingleValueEvent(new ValueEventListener() {

                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    isAvailable = dataSnapshot.hasChild(androidId);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }

        Log.d(TAG, "Helper is Available: " + Boolean.toString(isAvailable));
    }
}
