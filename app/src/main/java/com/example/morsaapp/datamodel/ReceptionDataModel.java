package com.example.morsaapp.datamodel;

import android.content.Context;
import android.database.Cursor;

import com.example.morsaapp.data.DBConnect;
import com.example.morsaapp.data.OdooData;

public class ReceptionDataModel {
    public String id;
    public String num;
    public String displayName;
    public String purchaseId;
    public String date;
    public String time;
    public String box;
    public String company;
    public String address;
    public String total;
    public String number;
    public boolean returnId;

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public String ref;
    public boolean show = true;

    public ReceptionDataModel(Context ctx, String id, String num) {
        this.id = id;
        this.num = num;
        DBConnect db = new DBConnect(ctx, OdooData.DBNAME,null,ctx.getSharedPreferences("startupPreferences", 0).getInt("DBver",1));
        String relatedId = "["+id+",\""+num+"\"]";
        Cursor cursor = db.getStockStatefromOrder(relatedId);
        while (cursor.moveToNext()){
            if(!cursor.getString(cursor.getColumnIndex("state")).equals("assigned")){
                show = false;
            }
        }
        cursor.close();
        db.close();
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getTotal() {
        return total;
    }

    public void setTotal(String total) {
        this.total = total;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) { this.company = company; }

    public String getId() { return id; }

    public void setId(String id) { this.id = id; }

    public String getNum() {
        return num;
    }

    public void setNum(String num) {
        this.num = num;
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

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPurchaseId() {
        return purchaseId;
    }

    public void setPurchaseId(String purchaseId) {
        this.purchaseId = purchaseId;
    }

    public boolean isReturnId() {
        return returnId;
    }

    public void setReturnId(boolean returnId) {
        this.returnId = returnId;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }
}
