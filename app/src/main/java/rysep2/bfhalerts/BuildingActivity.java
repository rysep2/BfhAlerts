package rysep2.bfhalerts;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rysep2.bfhalerts.DatabaseObjects.Building;
import rysep2.bfhalerts.DatabaseObjects.Floor;
import rysep2.bfhalerts.DatabaseObjects.Room;

public class BuildingActivity extends AppCompatActivity {

    private static final String TAG = AlertActivity.class.getSimpleName();

    private static final String BUILDINGS_CHILD = "buildings";
    private static final String FLOORS_CHILD = "floors";
    private static final String ROOMS_CHILD = "rooms";

    private DatabaseReference refMain;

    public List<Building> buildingsList;
    public List<String> buildingsNameList;
    public Building affectedBuilding;
    public Floor affectedFloor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_building);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        // Firebase initialisieren
        refMain = FirebaseDatabase.getInstance().getReference();

        buildingsList = new ArrayList<>();
        buildingsNameList = new ArrayList<>();
        buildingsNameList.add("");

        //Gebäudestrukturen holen
        refMain.child(BUILDINGS_CHILD).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot childBuilding : dataSnapshot.getChildren()) {
                    Building building = childBuilding.getValue(Building.class);

                    List<Floor> floorList = new ArrayList<>();
                    List<String> floorNameList = new ArrayList<>();
                    floorNameList.add("");
                    for (DataSnapshot childFloor : childBuilding.child(FLOORS_CHILD).getChildren()) {
                        Floor floor = childFloor.getValue(Floor.class);

                        List<String> roomList = new ArrayList<>();
                        roomList.add("");
                        for (DataSnapshot childRoom : childFloor.child(ROOMS_CHILD).getChildren()) {
                            roomList.add(childRoom.getValue(Room.class).getName());
                        }

                        floor.setId(childFloor.getKey());
                        floor.setRoomList(roomList);
                        floorList.add(floor);
                        floorNameList.add(floor.getName());
                    }

                    building.setId(childBuilding.getKey());
                    building.setFloorList(floorList);
                    building.setFloorNameList(floorNameList);
                    buildingsList.add(building);
                }

                updateBuildingsList();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        // Änderungen am Gebäude-Dropdown
        Spinner spnBuilding = (Spinner) findViewById(R.id.spnBuilding);
        spnBuilding.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                Spinner spnRoom = (Spinner) findViewById(R.id.spnRoom);
                spnRoom.setAdapter(null);
                TextView tvDisplay = (TextView) findViewById(R.id.tvDisplay);
                Button btnInput = (Button) findViewById(R.id.btnInput);

                if (parentView.getItemAtPosition(position).toString() == "") {
                    tvDisplay.setText("Neues Gebäude erfassen:");
                    btnInput.setEnabled(true);
                } else {
                    for (Building building : buildingsList) {
                        if (building.getName() == parentView.getItemAtPosition(position).toString()) {
                            if (building.getFloorNameList() != null) {

                                Spinner spnFloor = (Spinner) findViewById(R.id.spnFloor);
                                ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(BuildingActivity.this,
                                        android.R.layout.simple_spinner_item, building.getFloorNameList());
                                dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                                spnFloor.setAdapter(dataAdapter);
                            }

                            affectedBuilding = building;

                            tvDisplay.setText("Neues Stockwerk erfassen:");
                            btnInput.setEnabled(true);
                        }
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });

        // Änderungen am Stockwerk-Dropdown
        Spinner spnFloor = (Spinner) findViewById(R.id.spnFloor);
        spnFloor.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                TextView tvDisplay = (TextView) findViewById(R.id.tvDisplay);
                Button btnInput = (Button) findViewById(R.id.btnInput);

                if (parentView.getItemAtPosition(position).toString() == "") {
                    tvDisplay.setText("Neues Stockwerk erfassen:");
                    btnInput.setEnabled(true);
                } else {
                    for (Building building : buildingsList) {
                        if (building.getName() == affectedBuilding.getName()) {
                            for (Floor floor : building.getFloorList()) {
                                if (floor.getName() == parentView.getItemAtPosition(position).toString()) {

                                    if (floor.getRoomList() != null) {
                                        Spinner spnRoom = (Spinner) findViewById(R.id.spnRoom);
                                        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(BuildingActivity.this,
                                                android.R.layout.simple_spinner_item, floor.getRoomList());
                                        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                                        spnRoom.setAdapter(dataAdapter);
                                    }

                                    affectedFloor = floor;

                                    tvDisplay.setText("Neuer Raum erfassen:");
                                    btnInput.setEnabled(true);
                                }
                            }
                        }
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });

        // Änderungen am Raum-Dropdown
        Spinner spnRoom = (Spinner) findViewById(R.id.spnRoom);
        spnRoom.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                TextView tvDisplay = (TextView) findViewById(R.id.tvDisplay);
                Button btnInput = (Button) findViewById(R.id.btnInput);

                if (parentView.getItemAtPosition(position).toString() == "") {
                    tvDisplay.setText("Neuen Raum erfassen:");
                    btnInput.setEnabled(true);
                } else {
                    tvDisplay.setText("Unterkategorien von \"Raum\" können nich erfasst werden.");
                    btnInput.setEnabled(false);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });

        // Buttonclick für die Eingabe neuer Gebäudestruktur
        Button btnInput = (Button) findViewById(R.id.btnInput);
        btnInput.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText etInput = (EditText) findViewById(R.id.etInput);
                Spinner spnBuilding = (Spinner) findViewById(R.id.spnBuilding);
                Spinner spnFloor = (Spinner) findViewById(R.id.spnFloor);
                Spinner spnRoom = (Spinner) findViewById(R.id.spnRoom);

                String input = etInput.getText().toString();
                etInput.setText("");

                DatabaseReference mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();

                if (spnBuilding.getSelectedItem() == null || spnBuilding.getSelectedItem().toString() == "") {

                    // Neues Gebäude eingeben
                    Building newBuilding = new Building();
                    newBuilding.setName(input);

                    DatabaseReference refBuilding = mFirebaseDatabaseReference.child(BUILDINGS_CHILD).push();
                    refBuilding.setValue(newBuilding);
                    newBuilding.setId(refBuilding.getKey());

                    buildingsList.add(newBuilding);
                    updateBuildingsList();

                } else if (spnFloor.getSelectedItem() == null || spnFloor.getSelectedItem().toString() == "") {

                    // Neues Stockwerk eingeben
                    Floor newFloor = new Floor();
                    newFloor.setName(input);

                    DatabaseReference refFloor = mFirebaseDatabaseReference.child(BUILDINGS_CHILD).child(affectedBuilding.getId()).child(FLOORS_CHILD).push();
                    refFloor.setValue(newFloor);
                    newFloor.setId(refFloor.getKey());

                    updateFloorSpinner(newFloor, affectedBuilding.getFloorList() == null);

                } else if (spnRoom.getSelectedItem() == null || spnRoom.getSelectedItem().toString() == "") {

                    // Neuer Raum eingeben
                    Room newRoom = new Room();
                    newRoom.setName(input);

                    DatabaseReference refRoom = mFirebaseDatabaseReference.child(BUILDINGS_CHILD).child(affectedBuilding.getId()).child(FLOORS_CHILD).child(affectedFloor.getId()).child(ROOMS_CHILD).push();
                    refRoom.setValue(newRoom);

                    updateRoomSpinner(newRoom, affectedFloor.getRoomList() == null);
                }
            }
        });
    }

    private void updateBuildingsList() {
        buildingsNameList.clear();
        buildingsNameList.add("");
        for (Building building : buildingsList) {
            buildingsNameList.add(building.getName());
        }

        Collections.sort(buildingsNameList);

        Spinner spnBuilding = (Spinner) findViewById(R.id.spnBuilding);
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(BuildingActivity.this,
                android.R.layout.simple_spinner_item, buildingsNameList);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spnBuilding.setAdapter(dataAdapter);
    }

    private void updateFloorSpinner(Floor newFloor, boolean isFirst) {
        List<Floor> floorList = new ArrayList<>();
        if (!isFirst) floorList.addAll(affectedBuilding.getFloorList());
        floorList.add(newFloor);
        affectedBuilding.setFloorList(floorList);

        List<String> floorNameList = new ArrayList<>();
        if (!isFirst) floorNameList.addAll(affectedBuilding.getFloorNameList());
        floorNameList.add(newFloor.getName());
        affectedBuilding.setFloorNameList(floorNameList);

        if (isFirst) floorNameList.add("");
        Collections.sort(floorNameList);

        Spinner spnFloor = (Spinner) findViewById(R.id.spnFloor);
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(BuildingActivity.this,
                android.R.layout.simple_spinner_item, floorNameList);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spnFloor.setAdapter(dataAdapter);
    }

    private void updateRoomSpinner(Room newRoom, boolean isFirst) {

        List<String> roomNameList = new ArrayList<>();
        if (!isFirst) roomNameList.addAll(affectedFloor.getRoomList());
        roomNameList.add(newRoom.getName());
        affectedFloor.setRoomList(roomNameList);

        if (isFirst) roomNameList.add("");
        Collections.sort(roomNameList);

        Spinner spnRoom = (Spinner) findViewById(R.id.spnRoom);
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(BuildingActivity.this,
                android.R.layout.simple_spinner_item, roomNameList);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spnRoom.setAdapter(dataAdapter);
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
