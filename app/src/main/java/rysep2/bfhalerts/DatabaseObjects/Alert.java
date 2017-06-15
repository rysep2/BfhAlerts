package rysep2.bfhalerts.DatabaseObjects;

import android.content.Context;

import rysep2.bfhalerts.R;

/**
 * Created by Pascal on 07/01/2017.
 */

public class Alert {

    private String id;
    private String user;
    private long alertType;
    private long startTime;
    private boolean notificationSent;

    public Alert(){

    }

    public Alert(String user, long alertType, long startTime, boolean notificationSent) {
        this.user = user;
        this.alertType = alertType;
        this.startTime = startTime;
        this.notificationSent = notificationSent;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUser() {
        return user;
    }

    public long getAlertType() {
        return alertType;
    }

    public String getAlertTypeName(Context context) {
        return convertAlertTypeToName(alertType, context);
    }

    public long getStartTime() {
        return startTime;
    }

    public static String convertAlertTypeToName(long alertType, Context context) {
        String alertTypeName = "";
        switch((int) alertType){
            case 1:
                alertTypeName = context.getResources().getString(R.string.medicalEmergency);
                break;
            case 2:
                alertTypeName =  context.getResources().getString(R.string.fire);
                break;
            case 3:
                alertTypeName =  context.getResources().getString(R.string.flooding);
                break;
        }
        return alertTypeName;
    }

    public boolean getNotificationSent() {
        return notificationSent;
    }
}