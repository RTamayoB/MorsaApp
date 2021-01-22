package com.example.morsaapp.datamodel;

import java.io.Serializable;
import java.util.HashMap;

public class IssuesPopupDataModel implements Serializable {
    public int orderId;
    public String incid_type;
    private int incid_id;
    public Integer number = 0;
    private HashMap<Integer, Integer> incidHashMap = new HashMap<>();
    public IssuesPopupDataModel(){

    }

    public IssuesPopupDataModel(int orderId, String incid_type, int incid_id, Integer number) {
        this.orderId = orderId;
        this.incid_type = incid_type;
        this.incid_id = incid_id;
        this.number = number;
        this.incidHashMap = incidHashMap;
    }

    public int getIncid_id() {
        return incid_id;
    }

    public void setIncid_id(int incidId) {
        this.incid_id = incidId;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public HashMap<Integer, Integer> getIncidHashMap() {
        return incidHashMap;
    }

    public void setIncidHashMap(HashMap<Integer, Integer> incidHashMap) {
        this.incidHashMap = incidHashMap;
    }

    public String getIncid_type() {
        return incid_type;
    }

    public void setIncid_type(String incid_type) {
        this.incid_type = incid_type;
    }
}
