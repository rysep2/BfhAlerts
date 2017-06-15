package rysep2.bfhalerts.HelperClasses;

import android.annotation.TargetApi;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

/**
 * Created by Pascal on 03/04/2017.
 * Speichern der Anwesenheit für Versionen Android 7 Nougat und neuer.
 */

@TargetApi(Build.VERSION_CODES.N)
public class ConnectivityJob extends JobService {

    private static final String TAG = ConnectivityJob.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Service erstellt");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Service zerstört");
    }

    @Override
    public boolean onStartJob(final JobParameters params) {
        Log.i(TAG, "Job gestartet: " + params.getJobId());
        NetworkConnectivityHelper.handleAvailability(getBaseContext(), Settings.Secure.getString(getBaseContext().getContentResolver(), Settings.Secure.ANDROID_ID), "");
        jobFinished(params, true);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.i(TAG, "Job gestoppt: " + params.getJobId());
        return true;
    }
}
