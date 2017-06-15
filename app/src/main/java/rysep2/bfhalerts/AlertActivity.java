package rysep2.bfhalerts;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import rysep2.bfhalerts.DatabaseObjects.Answer;
import rysep2.bfhalerts.DatabaseObjects.Building;
import rysep2.bfhalerts.DatabaseObjects.Floor;
import rysep2.bfhalerts.DatabaseObjects.Message;
import rysep2.bfhalerts.DatabaseObjects.Question;
import rysep2.bfhalerts.DatabaseObjects.Room;

/**
 * Created by Pascal on 07/01/2017.
 */

public class AlertActivity extends AppCompatActivity {

    private static final String TAG = AlertActivity.class.getSimpleName();

    private static final String ALERTS_CHILD = "alerts";
    private static final String ARCHIVED_ALERTS_CHILD = "archived_alerts";
    private static final String MESSAGES_CHILD = "messages";

    private Button btnSendAnswer;
    private Button btnSendMessage;
    private RecyclerView messageRecyclerView;
    private LinearLayoutManager linearLayoutManager;
    private ProgressBar progressBar;
    private EditText txtMessage;

    private FirebaseRecyclerAdapter<Message, MessageViewHolder> messageFirebaseAdapter;
    private DatabaseReference refMain;
    private DatabaseReference refMessages;

    private SharedPreferences prefs;

    public List<Pair<String, String>> answerPairList;

    public List<Question> questionList;
    public List<Long> messageTypeList;
    public List<Building> buildingsList;

    public String affectedBuilding;
    public String affectedFloor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alert);

        //Mitgegebener Alert-Key und Alertname holen
        Intent intent = getIntent();
        final String ALERT_ID = intent.getStringExtra("alertId");
        final long alertTypeId = intent.getLongExtra("alertTypeId", 0);
        final boolean zusage = intent.getBooleanExtra("zusage", false);
        final boolean archived = intent.getBooleanExtra("archived", false);
        setTitle(intent.getStringExtra("alertTypeName"));

        // Firebase initialisieren
        refMain = FirebaseDatabase.getInstance().getReference();
        if (archived) {
            refMessages = refMain.child(ARCHIVED_ALERTS_CHILD).child(ALERT_ID).child(MESSAGES_CHILD);
            LinearLayout linlayMessaging = (LinearLayout) findViewById(R.id.linlayMessaging);
            linlayMessaging.setVisibility(View.GONE);
        } else {
            refMessages = refMain.child(ALERTS_CHILD).child(ALERT_ID).child(MESSAGES_CHILD);
        }

        prefs = PreferenceManager.getDefaultSharedPreferences(AlertActivity.this);

        // Bei direkter Zusage über Notification Info sofort versenden
        if (zusage) {
            Calendar c = Calendar.getInstance();
            long seconds = c.getTimeInMillis();

            Message message = new
                    Message("verpflichtet sich zur Hilfe: Ja",
                    prefs.getString("txtDisplayName", "Anonym"),
                    seconds,
                    0,
                    "Ja");
            refMessages.push().setValue(message);
        }

        buildingsList = new ArrayList<>();

        //Gebäudestrukturen holen
        refMain.child("buildings").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot childBuilding : dataSnapshot.getChildren()) {
                    Building building = childBuilding.getValue(Building.class);

                    List<Floor> floorList = new ArrayList<>();
                    List<String> floorNameList = new ArrayList<>();
                    for (DataSnapshot childFloor : childBuilding.child("floors").getChildren()) {
                        Floor floor = childFloor.getValue(Floor.class);

                        List<String> roomList = new ArrayList<>();
                        for (DataSnapshot childRoom : childFloor.child("rooms").getChildren()) {
                            roomList.add(childRoom.getValue(Room.class).getName());
                            answerPairList.add(new Pair<> (childRoom.getValue(Room.class).getName(), "Zimmer: %1$s"));
                        }

                        floor.setRoomList(roomList);
                        floorList.add(floor);
                        floorNameList.add(floor.getName());
                        answerPairList.add(new Pair<> (floor.getName(), "Stockwerk: %1$s"));
                    }

                    building.setFloorList(floorList);
                    building.setFloorNameList(floorNameList);
                    buildingsList.add(building);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        questionList = new ArrayList<>();
        messageTypeList = new ArrayList<>();
        answerPairList = new ArrayList<>();

        // Fragen und Antworten holen
        refMain.child("questions").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot childQuestion : dataSnapshot.getChildren()) {
                    Question question = childQuestion.getValue(Question.class);

                    if (question.getOnlyForHelper() == false || userIsHelper((int) alertTypeId) == true) {
                        List<String> answerList = new ArrayList<>();

                        for (DataSnapshot childAnswer : childQuestion.child("answers").getChildren()) {
                            Answer answer = childAnswer.getValue(Answer.class);
                            answerList.add(answer.getAnswer());
                            answerPairList.add(new Pair<> (answer.getAnswer(), question.getAnswerTextDE()));
                        }

                        question.setAnswerList(answerList);
                        questionList.add(question);
                    }
                }

                updateQuestion();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        // Update der Frage beim Eingang neuer Antworten
        refMessages.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot childMessage : dataSnapshot.getChildren()) {
                    Message message = childMessage.getValue(Message.class);

                    if (message.getMessageType() == 2) affectedBuilding = message.getMessageValue();
                    if (message.getMessageType() == 3) affectedFloor = message.getMessageValue();

                    if (!messageTypeList.contains(message.getMessageType())) {
                        messageTypeList.add(message.getMessageType());
                    }
                }

                updateQuestion();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        // Senden von Antworten ermöglichen
        btnSendAnswer = (Button) findViewById(R.id.btnSendAnswer);
        btnSendAnswer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Calendar c = Calendar.getInstance();
                long seconds = c.getTimeInMillis();

                // Nachricht versenden
                Spinner spinnerAnswer = (Spinner) findViewById(R.id.spinnerAnswer);
                Message message = new
                        Message(getAnswerText(spinnerAnswer.getSelectedItem().toString()),
                        prefs.getString("txtDisplayName", "Anonym"),
                        seconds,
                        questionList.get(0).getQuestionType(),
                        spinnerAnswer.getSelectedItem().toString().trim());
                refMessages.push().setValue(message);

            }
        });

        // ProgressBar und RecyclerView initialisieren
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        messageRecyclerView = (RecyclerView) findViewById(R.id.messageRecyclerView);
        linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        messageRecyclerView.setLayoutManager(linearLayoutManager);

        // Neue Message-Children
        DatabaseReference firebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
        messageFirebaseAdapter = new FirebaseRecyclerAdapter<Message, MessageViewHolder>(
                Message.class,
                R.layout.item_message,
                MessageViewHolder.class,
                refMessages) {

            @Override
            protected Message parseSnapshot(com.google.firebase.database.DataSnapshot snapshot) {
                Message message = super.parseSnapshot(snapshot);
                if (message != null) {
                    message.setId(snapshot.getKey());
                }
                return message;
            }

            @Override
            protected void populateViewHolder(MessageViewHolder viewHolder, final Message message, int position) {
                progressBar.setVisibility(ProgressBar.INVISIBLE);

                viewHolder.lblMessage.setText(message.getMessage());
                viewHolder.lblUser.setText(message.getUser());

                FirebaseAppIndex.getInstance().update(getMessageIndexable(message));
                FirebaseUserActions.getInstance().end(getMessageViewAction(message));
            }
        };

        messageFirebaseAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                int alertCount = messageFirebaseAdapter.getItemCount();
                int lastVisiblePosition =
                        linearLayoutManager.findLastCompletelyVisibleItemPosition();
                if (lastVisiblePosition == -1 ||
                        (positionStart >= (alertCount - 1) &&
                                lastVisiblePosition == (positionStart - 1))) {
                    messageRecyclerView.scrollToPosition(positionStart);
                }
            }
        });

        messageRecyclerView.setLayoutManager(linearLayoutManager);
        messageRecyclerView.setAdapter(messageFirebaseAdapter);

        // Senden von Messages ermöglichen
        txtMessage = (EditText) findViewById(R.id.txtMessage);
        btnSendMessage = (Button) findViewById(R.id.btnSendMessage);
        btnSendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!txtMessage.getText().toString().trim().isEmpty()) {
                    Calendar c = Calendar.getInstance();
                    long seconds = c.getTimeInMillis();

                    Message message = new
                            Message(txtMessage.getText().toString().trim(),
                            prefs.getString("txtDisplayName", "Anonym"),
                            seconds);
                    refMessages.push().setValue(message);
                    txtMessage.setText("");
                }
            }
        });
    }

    private Indexable getMessageIndexable(Message message) {
        Indexable messageToIndex = Indexables.messageBuilder()
                .setUrl(message.getId())
                .setName(message.getId())
                .build();

        return messageToIndex;
    }

    private Action getMessageViewAction(Message message) {
        return new Action.Builder(Action.Builder.VIEW_ACTION)
                .setObject(message.getId(), message.getId())
                .setMetadata(new Action.Metadata.Builder().setUpload(false))
                .build();
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        public TextView lblMessage;
        public TextView lblUser;

        public MessageViewHolder(View v) {
            super(v);
            lblMessage = (TextView) itemView.findViewById(R.id.lblMessage);
            lblUser = (TextView) itemView.findViewById(R.id.lblUser);
        }
    }


    private boolean userIsHelper(int alertTypeId) {
        // Herausfinden ob der Benutzer für den geöffneten Alarm ein Helfer ist
        Boolean isHelper = false;
        if (prefs.getBoolean("swiDoctor", false) && alertTypeId == 1) isHelper = true;
        if (prefs.getBoolean("swiFirefighter", false) && alertTypeId == 2) isHelper = true;
        if (prefs.getBoolean("swiJanitors", false) && alertTypeId == 3) isHelper = true;

        return isHelper;
    }

    public void updateQuestion() {
        TextView txtQuestion = (TextView) findViewById(R.id.txtQuestion);
        Spinner spinnerAnswer = (Spinner) findViewById(R.id.spinnerAnswer);

        int questionListSizeBefore = questionList.size();

        // Schon gestellte Fragen aus der Liste entfernen
        List<Long> deletionList = new ArrayList<>();
        long i = 0;
        for (Question question : questionList) {
            if (messageTypeList.contains(question.getQuestionType())) {
                deletionList.add(i);
            }
            i++;
        }

        Collections.reverse(deletionList);

        for (long deletionIndex : deletionList) {
            questionList.remove((int) deletionIndex);
        }

        if (questionList.size() != 0) {

            Question relevantQuestion = questionList.get(0);

            txtQuestion.setText(relevantQuestion.getQuestion());

            // Stockwerk und Zimmer haben Sonderstatus, da sie von einander Abhängig sind (Gebäude/Stock/Zimmer)
            switch ((int) relevantQuestion.getQuestionType()) {
                case 3:
                    ArrayAdapter<String> floorAdapter = new ArrayAdapter<>(AlertActivity.this,
                            android.R.layout.simple_spinner_item, GetTypeThreeValues());
                    floorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                    spinnerAnswer.setAdapter(floorAdapter);
                    break;

                case 4:
                    ArrayAdapter<String> roomAdapter = new ArrayAdapter<>(AlertActivity.this,
                            android.R.layout.simple_spinner_item, GetTypeFourValues());
                    roomAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                    spinnerAnswer.setAdapter(roomAdapter);
                    break;

                default:
                    ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(AlertActivity.this,
                            android.R.layout.simple_spinner_item, relevantQuestion.getAnswerList());
                    dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                    spinnerAnswer.setAdapter(dataAdapter);
                    break;
            }

        } else if (questionListSizeBefore >= 1 && questionList.size() == 0) {

            txtQuestion.setText("Vielen Dank für Ihre Mithilfe!");
            spinnerAnswer.setVisibility(View.INVISIBLE);
            btnSendAnswer.setVisibility(View.INVISIBLE);

        }
    }

    private List<String> GetTypeThreeValues() {
        // Stockwerk-Liste bestimmen
        List<String> floorList = new ArrayList<>();
        for (Building building : buildingsList){
            if (building.getName().equals(affectedBuilding)){
                floorList.addAll(building.getFloorNameList());
            }
        }
        return floorList;
    }

    private List<String> GetTypeFourValues() {
        // Raumliste bestimmen
        List<String> roomList = new ArrayList<>();
        for (Building building : buildingsList){
            if (building.getName().equals(affectedBuilding)){
                for (Floor floor : building.getFloorList()){
                    if (floor.getName().equals(affectedFloor)){
                        roomList.addAll(floor.getRoomList());
                    }
                }
            }
        }
        return roomList;
    }

    private String getAnswerText(String selectedAnswer) {
        String answerText = "";
        for (Pair<String, String> answerPair : answerPairList){
            if (answerPair.first.equals(selectedAnswer)){
                answerText = String.format(answerPair.second, selectedAnswer);
            }
        }
        return answerText;
    }
}
