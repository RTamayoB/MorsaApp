package com.example.morsaapp.datamodel;

public class MissingProductsDatamodel {

    public int id;
    public String name;
    public int qty;
    public Integer totalQty;
    public boolean isChecked = false;
    public Integer missingQty;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getQty() {
        return qty;
    }

    public void setQty(int qty) {
        this.qty = qty;
    }

    public Integer getTotalQty() {
        return totalQty;
    }

    public void setTotalQty(Integer totalQty) {
        this.totalQty = totalQty;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }

    public Integer getMissingQty() {
        return missingQty;
    }

    public void setMissingQty(Integer missingQty) {
        this.missingQty = missingQty;
    }
}
