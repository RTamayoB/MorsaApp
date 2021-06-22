package com.example.morsaapp.datamodel;

public class OrderRevisionDataModel {
    public int Id;
    public String productName;
    public int qty;
    public int productId;
    public String incidencies;
    public int revisionQty = 0;
    public String relabel;
    public int lineScanned = 0;

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public int getRevisionQty() {
        return revisionQty;
    }

    public void setRevisionQty(int revisionQty) {
        this.revisionQty = revisionQty;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public int getQty() {
        return qty;
    }

    public void setQty(int qty) {
        this.qty = qty;
    }

    public String getIncidencies() {
        return incidencies;
    }

    public void setIncidencies(String incidencies) {
        this.incidencies = incidencies;
    }

    public String getRelabel() {
        return relabel;
    }

    public void setRelabel(String relabel) {
        this.relabel = relabel;
    }
}
