package rysep2.bfhalerts;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.appindexing.Action;
import com.google.firebase.appindexing.FirebaseAppIndex;
import com.google.firebase.appindexing.FirebaseUserActions;
import com.google.firebase.appindexing.Indexable;
import com.google.firebase.appindexing.builders.Indexables;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import rysep2.bfhalerts.DatabaseObjects.Alert;

public class ArchiveActivity extends AppCompatActivity {

    private static String URL;
    private static String ARCHIVED_ALERTS_CHILD;
    private FirebaseRecyclerAdapter<Alert, MainActivity.AlertViewHolder> mFirebaseAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_archive);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        URL = getResources().getString(R.string.firebaseAlertsUrl);
        ARCHIVED_ALERTS_CHILD = getResources().getString(R.string.firebaseArchivedAlerts);

        // RecyclerView initialisieren
        final RecyclerView alertRecyclerView = (RecyclerView) findViewById(R.id.alertArchiveRecyclerView);
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setReverseLayout(true);
        alertRecyclerView.setLayoutManager(linearLayoutManager);

        // Neue Alert-Children
        DatabaseReference firebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
        mFirebaseAdapter = new FirebaseRecyclerAdapter<Alert, MainActivity.AlertViewHolder>(
                Alert.class,
                R.layout.item_alert,
                MainActivity.AlertViewHolder.class,
                firebaseDatabaseReference.child(ARCHIVED_ALERTS_CHILD)) {

            @Override
            protected Alert parseSnapshot(com.google.firebase.database.DataSnapshot snapshot) {
                Alert alert = super.parseSnapshot(snapshot);
                if (alert != null) {
                    alert.setId(snapshot.getKey());
                }
                return alert;
            }

            @Override
            protected void populateViewHolder(MainActivity.AlertViewHolder viewHolder, final Alert alert, int position) {
                //progressBar.setVisibility(ProgressBar.INVISIBLE);
                viewHolder.tvAlertName.setText(alert.getAlertTypeName(getBaseContext()));

                long seconds = alert.getStartTime();
                viewHolder.tvAlertDate.setText(MainActivity.getDate(seconds, "dd.MM.yyyy HH:mm:ss"));
                viewHolder.linlayAlertItem.setBackgroundColor(ResourcesCompat.getColor(getResources(), R.color.colorAccent, null));

                viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        Intent alertIntent = new Intent(ArchiveActivity.this, AlertActivity.class);
                        alertIntent.putExtra("alertId", alert.getId());
                        alertIntent.putExtra("alertTypeId", alert.getAlertType());
                        alertIntent.putExtra("alertTypeName", alert.getAlertTypeName(getBaseContext()));
                        alertIntent.putExtra("archived", true);
                        startActivity(alertIntent);
                    }
                });

                FirebaseAppIndex.getInstance().update(getMessageIndexable(alert));
                // log a view action on it
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
                // If the recycler view is initially being loaded or the
                // user is at the bottom of the list, scroll to the bottom
                // of the list to show the newly added message.
                if (lastVisiblePosition == -1 ||
                        (positionStart >= (alertCount - 1) &&
                                lastVisiblePosition == (positionStart - 1))) {
                    alertRecyclerView.scrollToPosition(positionStart);
                }
            }
        });

        alertRecyclerView.setLayoutManager(linearLayoutManager);
        alertRecyclerView.setAdapter(mFirebaseAdapter);
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
