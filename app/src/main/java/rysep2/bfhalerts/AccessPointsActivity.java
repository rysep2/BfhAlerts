package rysep2.bfhalerts;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import rysep2.bfhalerts.DatabaseObjects.Building;

public class AccessPointsActivity extends AppCompatActivity {
    private static final String BUILDINGS_CHILD = "buildings";
    private static final String ACCESS_POINTS = "access_points";

    private String currentBssid;
    private DatabaseReference refMain;
    public List<String> buildingsNameList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_access_points);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        // Firebase initialisieren
        refMain = FirebaseDatabase.getInstance().getReference();

        buildingsNameList = new ArrayList<>();

        //Gebäudeliste holen
        refMain.child(BUILDINGS_CHILD).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot childBuilding : dataSnapshot.getChildren()) {

                    Building building = childBuilding.getValue(Building.class);
                    buildingsNameList.add(building.getName());

                }

                Spinner spnBssid = (Spinner) findViewById(R.id.spnBssid);
                ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(AccessPointsActivity.this,
                        android.R.layout.simple_spinner_item, buildingsNameList);
                dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                spnBssid.setAdapter(dataAdapter);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


        // BSSID holen
        Button btnGetAccessPoint;
        btnGetAccessPoint = (Button) findViewById(R.id.btnGetAccessPoint);
        btnGetAccessPoint.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                TextView tvBSSID = (TextView) findViewById(R.id.tvBSSID);

                WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                currentBssid = wifiInfo.getBSSID();

                tvBSSID.setText("BSSID: " + currentBssid);

            }
        });

        // BSSID speichern
        Button btnSetAccessPoint;
        btnSetAccessPoint = (Button) findViewById(R.id.btnSetAccessPoint);
        btnSetAccessPoint.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                Spinner spnBssid = (Spinner) findViewById(R.id.spnBssid);
                String location = spnBssid.getSelectedItem().toString();

                DatabaseReference mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
                mFirebaseDatabaseReference.child(ACCESS_POINTS).child(currentBssid.substring(0, 8)).child("standort").setValue(location);

            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
