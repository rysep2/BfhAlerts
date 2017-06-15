package rysep2.bfhalerts;


import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.List;

import rysep2.bfhalerts.HelperClasses.NetworkConnectivityHelper;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity {

    public static DatabaseReference refFirebase;
    public static String androidId;
    public static int versionClick;

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else if (preference instanceof RingtonePreference) {
                // For ringtone preferences, look up the correct display value
                // using RingtoneManager.
                if (TextUtils.isEmpty(stringValue)) {
                    // Empty values correspond to 'silent' (no ringtone).
                    preference.setSummary(R.string.pref_ringtone_silent);

                } else {
                    Ringtone ringtone = RingtoneManager.getRingtone(
                            preference.getContext(), Uri.parse(stringValue));

                    if (ringtone == null) {
                        // Clear the summary if there was a lookup error.
                        preference.setSummary(null);
                    } else {
                        // Set the summary to reflect the new ringtone display
                        // name.
                        String name = ringtone.getTitle(preference.getContext());
                        preference.setSummary(name);
                    }
                }

            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        refFirebase = FirebaseDatabase.getInstance().getReference();
        androidId = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
        setupActionBar();
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || GeneralPreferenceFragment.class.getName().equals(fragmentName)
                || NotificationPreferenceFragment.class.getName().equals(fragmentName);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);
            setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("txtDisplayName"));

            SwitchPreference swiDoctor = (SwitchPreference) findPreference("swiDoctor");
            SwitchPreference swiFirefighter = (SwitchPreference) findPreference("swiFirefighter");
            SwitchPreference swiJanitor = (SwitchPreference) findPreference("swiJanitor");

            findPreference("swiDoctor").setEnabled(true);
            findPreference("swiFirefighter").setEnabled(true);
            findPreference("swiJanitor").setEnabled(true);

            if (swiDoctor.isChecked()) {
                swiFirefighter.setEnabled(false);
                swiJanitor.setEnabled(false);
            }
            if (swiFirefighter.isChecked()) {
                swiDoctor.setEnabled(false);
                swiJanitor.setEnabled(false);
            }
            if (swiJanitor.isChecked()) {
                swiDoctor.setEnabled(false);
                swiFirefighter.setEnabled(false);
            }

            swiDoctor.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (((SwitchPreference) preference).isChecked()) {
                        unregister("doctors", 1);
                    } else {
                        register("doctors", 1);
                    }
                    return true;
                }
            });
            swiFirefighter.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (((SwitchPreference) preference).isChecked()) {
                        unregister("firefighters", 2);
                    } else {
                        register("firefighters", 2);
                    }
                    return true;
                }
            });
            swiJanitor.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (((SwitchPreference) preference).isChecked()) {
                        unregister("janitors", 3);
                    } else {
                        register("janitors", 3);
                    }
                    return true;
                }
            });

            Preference btnAbout = findPreference("btnAbout");
            btnAbout.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    versionClick++;
                    if (versionClick == 5) {
                        SharedPreferences sharedPref = getActivity().getSharedPreferences(getString(R.string.adminMode), MODE_PRIVATE);

                        if (sharedPref.getBoolean(getString(R.string.adminMode), false) == false) {

                            SharedPreferences.Editor editor = sharedPref.edit();
                            editor.putBoolean(getString(R.string.adminMode), true);
                            editor.commit();

                            Toast toast = Toast.makeText(getActivity().getApplicationContext(), "Adminmodus aktiviert", Toast.LENGTH_SHORT);
                            toast.show();

                        } else {

                            SharedPreferences.Editor editor = sharedPref.edit();
                            editor.putBoolean(getString(R.string.adminMode), false);
                            editor.commit();

                            Toast toast = Toast.makeText(getActivity().getApplicationContext(), "Adminmodus deaktiviert", Toast.LENGTH_SHORT);
                            toast.show();

                        }
                    }
                    return true;
                }
            });
        }

        public void register(String helper, int alertType) {
            // Topic abonnieren um entsprechende Notifications zu erhalten
            FirebaseMessaging.getInstance().subscribeToTopic("alerttype_" + alertType);

            // In Firebase speichern
            DatabaseReference mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
            mFirebaseDatabaseReference.child("roles").child(helper).child(androidId).setValue("true");
            NetworkConnectivityHelper.handleAvailability(getActivity().getApplicationContext(), Settings.Secure.getString(getActivity().getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID), helper);

            // Andere Helfer-Switches ausschalten
            if (helper != "doctors") findPreference("swiDoctor").setEnabled(false);
            if (helper != "firefighters") findPreference("swiFirefighter").setEnabled(false);
            if (helper != "janitors") findPreference("swiJanitor").setEnabled(false);

            // Info-Popup anzeigen
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(R.string.registerDialogMessage)
                    .setTitle(R.string.registerDialogTitle);
            AlertDialog dialog = builder.create();
            dialog.show();
        }

        public void unregister(String helper, int alertType) {
            // Topic unsubscriben um entsprechende Notifications nicht mehr zu erhalten
            FirebaseMessaging.getInstance().unsubscribeFromTopic("alerttype_" + alertType);

            // In Firebase löschen
            DatabaseReference mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
            mFirebaseDatabaseReference.child("roles").child(helper).child(androidId).removeValue();
            mFirebaseDatabaseReference.child("availabilities").child(helper).child(androidId).removeValue();

            // Andere Helfer-Switches einschalten
            if (helper != "doctors") findPreference("swiDoctor").setEnabled(true);
            if (helper != "firefighters") findPreference("swiFirefighter").setEnabled(true);
            if (helper != "janitors") findPreference("swiJanitor").setEnabled(true);
        }
    }

    /**
     * This fragment shows notification preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class NotificationPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_notification);
            setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("notifications_new_message_ringtone"));
        }
    }
}
