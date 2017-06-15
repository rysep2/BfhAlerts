package rysep2.bfhalerts.DatabaseObjects;

/**
 * Created by Pascal on 07/01/2017.
 */

public class Message {

    private String id;
    private String message;
    private String user;
    private long time;
    private long messageType;
    private String messageValue;

    public Message() {
    }

    public Message(String message, String user, long time) {
        this.message = message;
        this.user = user;
        this.time = time;
    }

    public Message(String message, String user, long time, long messageType, String messageValue) {
        this.message = message;
        this.user = user;
        this.time = time;
        this.messageType = messageType;
        this.messageValue = messageValue;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public String getUser() {
        return user;
    }

    public long getTime() {
        return time;
    }

    public long getMessageType() {
        return messageType;
    }
    public String getMessageValue() {
        return messageValue;
    }
}
