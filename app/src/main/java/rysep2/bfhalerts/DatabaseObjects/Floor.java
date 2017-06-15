package rysep2.bfhalerts.DatabaseObjects;

import java.util.List;

/**
 * Created by Pascal on 08/04/2017.
 */

public class Floor {

    private String id;
    private String name;
    private List<String> roomList;

    public Floor(){

    }

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name){
        this.name = name;
    }
    public void setRoomList(List<String> roomList){
        this.roomList = roomList;
    }
    public List<String> getRoomList(){
        return roomList;
    }
}
