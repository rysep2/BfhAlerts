package rysep2.bfhalerts;

import android.app.AlertDialog;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.FirebaseApp;
import com.google.firebase.appindexing.Action;
import com.google.firebase.appindexing.FirebaseAppIndex;
import com.google.firebase.appindexing.FirebaseUserActions;
import com.google.firebase.appindexing.Indexable;
import com.google.firebase.appindexing.builders.Indexables;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import rysep2.bfhalerts.DatabaseObjects.Alert;
import rysep2.bfhalerts.DatabaseObjects.Message;
import rysep2.bfhalerts.HelperClasses.ConnectivityJob;

import static android.app.job.JobInfo.getMinPeriodMillis;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static String URL;
    private static String ALERTS_CHILD;
    private static String MESSAGES_CHILD;
    private static String ACCESS_POINTS_CHILD;
    private static String AVAILABILITIES_CHILD;
    private static String DOCTORS_CHILD;
    private static String FIREFIGHTERS_CHILD;
    private static String JANITORS_CHILD;

    private static final int MY_PERMISSIONS_REQUEST_CALL_PHONE = 0;

    private DatabaseReference refMain;
    private DatabaseReference refAlerts;
    private DatabaseReference refAlert;
    private FirebaseRecyclerAdapter<Alert, AlertViewHolder> mFirebaseAdapter;

    private RadioGroup rdbGroupEmergency;
    private Button btnAlarm;
    private ValueEventListener alertListener;
    private ProgressBar progressBar;

    private String currentLocation;
    private SharedPreferences prefs;

    private String currentAlertKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Konstanten aus den Resourcen holen
        URL = getResources().getString(R.string.firebaseAlertsUrl);
        ALERTS_CHILD = getResources().getString(R.string.firebaseAlerts);
        MESSAGES_CHILD = getResources().getString(R.string.firebaseMessages);
        ACCESS_POINTS_CHILD = getResources().getString(R.string.firebaseAccessPoints);
        AVAILABILITIES_CHILD = getResources().getString(R.string.firebaseAvailabilities);
        DOCTORS_CHILD = getResources().getString(R.string.firebaseDoctors);
        FIREFIGHTERS_CHILD = getResources().getString(R.string.firebaseFirefighters);
        JANITORS_CHILD = getResources().getString(R.string.firebaseJanitors);

        if (Build.VERSION.SDK_INT >= 11) {
            invalidateOptionsMenu();
        }

        // Firebase initialisieren
        FirebaseApp.initializeApp(this);
        refMain = FirebaseDatabase.getInstance().getReference();
        refAlerts = refMain.child(ALERTS_CHILD);

        // Job Initialisieren für neuere Versionen
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            JobScheduler mJobScheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
            JobInfo.Builder builder = new JobInfo.Builder(1, new ComponentName(getPackageName(), ConnectivityJob.class.getName()));
            builder.setPersisted(true);
            builder.setPeriodic(getMinPeriodMillis());
            //builder.setTriggerContentMaxDelay(30000);
            if (mJobScheduler.schedule(builder.build()) <= 0) {
                Log.e(TAG, "Fehler beim initialisieren des Schedulers");
            }
        }

        // Anwesenheiten anzeigen
        refMain.child(AVAILABILITIES_CHILD).addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                RadioButton rdbMedicalEmergency = (RadioButton) findViewById(R.id.rdbMedicalEmergency);
                RadioButton rdbFire = (RadioButton) findViewById(R.id.rdbFire);
                RadioButton rdbFlooding = (RadioButton) findViewById(R.id.rdbFlooding);

                rdbMedicalEmergency.setText(getHelperAvailabilityString(getResources().getString(R.string.medicalEmergency), (int) dataSnapshot.child(DOCTORS_CHILD).getChildrenCount()));
                rdbFire.setText(getHelperAvailabilityString(getResources().getString(R.string.fire), (int) dataSnapshot.child(FIREFIGHTERS_CHILD).getChildrenCount()));
                rdbFlooding.setText(getHelperAvailabilityString(getResources().getString(R.string.flooding), (int) dataSnapshot.child(JANITORS_CHILD).getChildrenCount()));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        // Alarmknopf enablen wenn eine Notfallart gewählt wurde
        rdbGroupEmergency = (RadioGroup) findViewById(R.id.rdbGroupEmergency);
        rdbGroupEmergency.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                btnAlarm.setEnabled(true);
            }
        });

        // Alarmknopf initialisieren
        btnAlarm = (Button) findViewById(R.id.btnStartAlert);
        btnAlarm.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                Calendar c = Calendar.getInstance();
                long seconds = c.getTimeInMillis();

                Alert alert = null;
                Boolean isMedicalAlert = false;
                switch (rdbGroupEmergency.getCheckedRadioButtonId()) {
                    case R.id.rdbMedicalEmergency:
                        alert = new Alert(prefs.getString("txtDisplayName", "Anonym"), 1, seconds, false);
                        isMedicalAlert = true;
                        break;
                    case R.id.rdbFire:
                        alert = new Alert(prefs.getString("txtDisplayName", "Anonym"), 2, seconds, false);
                        break;
                    case R.id.rdbFlooding:
                        alert = new Alert(prefs.getString("txtDisplayName", "Anonym"), 3, seconds, false);
                        break;
                }
                refAlert = refAlerts.push();
                refAlert.setValue(alert);

                Message message = new Message("Neuer Alarm gestartet", prefs.getString("txtDisplayName", "Anonym"), seconds);
                refAlert.child(MESSAGES_CHILD).push().setValue(message);

                currentAlertKey = refAlert.getKey();

                rdbGroupEmergency.clearCheck();
                btnAlarm.setEnabled(false);

                WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                String bssid = wifiInfo.getBSSID();

                if (bssid != null) {
                    refMain.child(ACCESS_POINTS_CHILD).child(bssid.substring(0, 8)).addListenerForSingleValueEvent(new ValueEventListener() {

                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (dataSnapshot.hasChildren()) {
                                DataSnapshot child = dataSnapshot.getChildren().iterator().next();
                                currentLocation = child.getValue().toString();

                                Calendar c = Calendar.getInstance();
                                long seconds = c.getTimeInMillis();
                                prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);

                                Message infoLocation = new Message("Standort " + currentLocation, prefs.getString("txtDisplayName", "Anonym"), seconds, 2, currentLocation);
                                refAlert.child(MESSAGES_CHILD).push().setValue(infoLocation);
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                }

                // Falls es sich um einen medizinischen Notfall handelt: Dialog "144 anrufen" anzeigen
                if (isMedicalAlert) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            Intent intent = new Intent(Intent.ACTION_CALL);
                            intent.setData(Uri.parse("tel:" + R.string.emergencyNumber));

                            // Permission für automatisches Anrufen abfragen
                            if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                                ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.CALL_PHONE}, MY_PERMISSIONS_REQUEST_CALL_PHONE);
                                return;
                            }

                            // Anruf tätigen
                            getBaseContext().startActivity(intent);

                        }
                    });
                    builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {

                            // Alarm Activity öffnen
                            Intent alertIntent = new Intent(MainActivity.this, AlertActivity.class);
                            alertIntent.putExtra("alertId", currentAlertKey);
                            alertIntent.putExtra("alertTypeId", 1);
                            alertIntent.putExtra("alertTypeName", getResources().getString(R.string.medicalEmergency));
                            startActivity(alertIntent);

                        }
                    });
                    builder.setMessage(R.string.alertCallDialogMessage)
                            .setTitle(R.string.alertCallDialogTitle);
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
            }
        });

        // ProgressBar und RecyclerView initialisieren
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        final RecyclerView alertRecyclerView = (RecyclerView) findViewById(R.id.alertRecyclerView);
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setReverseLayout(true);
        alertRecyclerView.setLayoutManager(linearLayoutManager);

        // Neue Alert-Children
        DatabaseReference firebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
        mFirebaseAdapter = new FirebaseRecyclerAdapter<Alert, AlertViewHolder>(
                Alert.class,
                R.layout.item_alert,
                AlertViewHolder.class,
                firebaseDatabaseReference.child(ALERTS_CHILD)) {

            @Override
            protected Alert parseSnapshot(com.google.firebase.database.DataSnapshot snapshot) {
                Alert alert = super.parseSnapshot(snapshot);
                if (alert != null) {
                    alert.setId(snapshot.getKey());
                }
                return alert;
            }

            @Override
            protected void populateViewHolder(AlertViewHolder viewHolder, final Alert alert, int position) {
                progressBar.setVisibility(ProgressBar.INVISIBLE);
                viewHolder.tvAlertName.setText(alert.getAlertTypeName(getBaseContext()));

                long seconds = alert.getStartTime();
                viewHolder.tvAlertDate.setText(getDate(seconds, "dd.MM.yyyy HH:mm:ss"));

                // andere Farbe wenn der Eintrag älter als 15 Minuten ist.
                if (olderThanFifteenMinutes(seconds)) {
                    viewHolder.linlayAlertItem.setBackgroundColor(ResourcesCompat.getColor(getResources(), R.color.colorAccent, null));
                }

                viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        Intent alertIntent = new Intent(MainActivity.this, AlertActivity.class);
                        alertIntent.putExtra("alertId", alert.getId());
                        alertIntent.putExtra("alertTypeId", alert.getAlertType());
                        alertIntent.putExtra("alertTypeName", alert.getAlertTypeName(getBaseContext()));
                        startActivity(alertIntent);
                    }
                });

                FirebaseAppIndex.getInstance().update(getMessageIndexable(alert));
                FirebaseUserActions.getInstance().end(getMessageViewAction(alert));
            }
        };

        mFirebaseAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                int alertCount = mFirebaseAdapter.getItemCount();
                int lastVisiblePosition =
                        linearLayoutManager.findLastCompletelyVisibleItemPosition();
                if (lastVisiblePosition == -1 ||
                        (positionStart >= (alertCount - 1) &&
                                lastVisiblePosition == (positionStart - 1))) {
                    alertRecyclerView.scrollToPosition(positionStart);
                }
            }
        });

        alertRecyclerView.setLayoutManager(linearLayoutManager);
        alertRecyclerView.setAdapter(mFirebaseAdapter);

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    private boolean olderThanFifteenMinutes(long seconds) {
        boolean isOlder = false;
        long currentTimeMinusFifteenMinutes = System.currentTimeMillis() - 15 * 60 * 1000;

        if (currentTimeMinusFifteenMinutes >= seconds) {
            isOlder = true;
        }

        return isOlder;
    }

    private String getHelperAvailabilityString(String emergency, int childrenCount) {
        StringBuilder sb = new StringBuilder();
        sb.append(emergency);
        sb.append(" (");
        sb.append(childrenCount);
        sb.append(" Helfer anwesend)");
        return sb.toString();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Log.e(TAG, "NewIntent");
        Toast.makeText(this, "New Intent", Toast.LENGTH_LONG);
    }

    private Indexable getMessageIndexable(Alert alert) {
        Indexable messageToIndex = Indexables.messageBuilder()
                .setName(alert.getId())
                .setUrl(alert.getId())
                .build();

        return messageToIndex;
    }

    private Action getMessageViewAction(Alert alert) {
        return new Action.Builder(Action.Builder.VIEW_ACTION)
                .setObject(alert.getId(), URL.concat(alert.getId()))
                .setMetadata(new Action.Metadata.Builder().setUpload(false))
                .build();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        if (Build.VERSION.SDK_INT >= 11) {
            selectMenu(menu);
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (Build.VERSION.SDK_INT < 11) {
            selectMenu(menu);
        }
        return true;
    }

    private void selectMenu(Menu menu) {
        SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.adminMode), MODE_PRIVATE);
        boolean isAdmin = sharedPref.getBoolean(getString(R.string.adminMode), false);

        menu.clear();
        MenuInflater inflater = getMenuInflater();

        if (isAdmin) {
            inflater.inflate(R.menu.main_menu_admin, menu);
        }
        else {
            inflater.inflate(R.menu.main_menu, menu);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_CALL_PHONE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {


                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //Menü aufbauen

        switch (item.getItemId()) {
            case R.id.access_points:
                Intent accessPointsIntent = new Intent(this, AccessPointsActivity.class);
                this.startActivity(accessPointsIntent);
                return true;

            case R.id.buildings:
                Intent buildingsIntent = new Intent(this, BuildingActivity.class);
                this.startActivity(buildingsIntent);
                return true;

            case R.id.archived_alerts:
                Intent archivedAlertsIntent = new Intent(this, ArchiveActivity.class);
                this.startActivity(archivedAlertsIntent);
                return true;

            case R.id.properties:
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                this.startActivity(settingsIntent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Return date in specified format.
     * @param milliSeconds Date in milliseconds
     * @param dateFormat Date format
     * @return String representing date in specified format
     */
    public static String getDate(long milliSeconds, String dateFormat)
    {
        SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milliSeconds);
        return formatter.format(calendar.getTime());
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public com.google.android.gms.appindexing.Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Alerts")
                .setUrl(Uri.parse("https://bfhalerts.firebaseio.com/"))
                .build();
        return new com.google.android.gms.appindexing.Action.Builder(com.google.android.gms.appindexing.Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(com.google.android.gms.appindexing.Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (Build.VERSION.SDK_INT >= 11) {
            invalidateOptionsMenu();
        }
    }

    public static class AlertViewHolder extends RecyclerView.ViewHolder {
        public LinearLayout linlayAlertItem;
        public TextView tvAlertName;
        public TextView tvAlertDate;

        public AlertViewHolder(View v) {
            super(v);
            linlayAlertItem = (LinearLayout) itemView.findViewById(R.id.linlayAlertItem);
            tvAlertName = (TextView) itemView.findViewById(R.id.tvAlertName);
            tvAlertDate = (TextView) itemView.findViewById(R.id.tvAlertDate);
        }
    }
}