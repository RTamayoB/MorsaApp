package com.example.morsaapp.datamodel;

public class PickingDataModel {
    public String id;
    public String picking_ids;
    public String name;
    public String date;
    public String time;
    public String box;
    public String orderType;


    public PickingDataModel() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPicking_ids() {
        return picking_ids;
    }

    public void setPicking_ids(String picking_ids) {
        this.picking_ids = picking_ids;
    }

    public String getName() {
        return name;
    }

    public void setNames(String names) {
        this.name = names;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getBox() {
        return box;
    }

    public void setBox(String box) {
        this.box = box;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }
}
