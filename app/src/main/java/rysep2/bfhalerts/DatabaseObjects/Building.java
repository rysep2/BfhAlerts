package rysep2.bfhalerts.DatabaseObjects;

import java.util.List;

/**
 * Created by Pascal on 08/04/2017.
 */

public class Building {

    private String id;
    private String name;
    private List<Floor> floorList;
    private List<String> floorNameList;

    public Building(){

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
    public void setFloorList(List<Floor> floorList){
        this.floorList = floorList;
    }
    public List<Floor> getFloorList(){
        return floorList;
    }
    public void setFloorNameList(List<String> floorNameList){
        this.floorNameList = floorNameList;
    }
    public List<String> getFloorNameList(){
        return floorNameList;
    }
}
