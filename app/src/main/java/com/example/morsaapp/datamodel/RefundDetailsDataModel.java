package com.example.morsaapp.datamodel;

public class RefundDetailsDataModel {
    public int Id;
    public String name;
    public String productName;
    public int productId;
    public int qty;
    public int revisionQty = 0;
    public int lineScanned = 0;

    public int getId() {
        return Id;
    }

    public void setId(int id) {
        Id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public int getQty() {
        return qty;
    }

    public void setQty(int qty) {
        this.qty = qty;
    }

    public int getRevisionQty() {
        return revisionQty;
    }

    public void setRevisionQty(int revisionQty) {
        this.revisionQty = revisionQty;
    }

    public int getLineScanned() {
        return lineScanned;
    }

    public void setLineScanned(int lineScanned) {
        this.lineScanned = lineScanned;
    }
}
