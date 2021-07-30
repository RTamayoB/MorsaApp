package com.example.morsaapp.datamodel;

public class ReStockDataModel {
    public int Id;
    public String reProductId;
    public String reProduct;
    public String reOrigin;
    public String reDestiny;
    public int reQty;
    public int reTotalQty;
    public int isCounted;
    public boolean originScanned;

    public int getId() {
        return Id;
    }

    public void setId(int id) {
        Id = id;
    }

    public String getReProductId() {
        return reProductId;
    }

    public void setReProductId(String reProductId) {
        this.reProductId = reProductId;
    }

    public String getReProduct() {
        return reProduct;
    }

    public void setReProduct(String reProduct) {
        this.reProduct = reProduct;
    }

    public String getReOrigin() {
        return reOrigin;
    }

    public void setReOrigin(String reOrigin) {
        this.reOrigin = reOrigin;
    }

    public String getReDestiny() {
        return reDestiny;
    }

    public void setReDestiny(String reDestiny) {
        this.reDestiny = reDestiny;
    }

    public int getReQty() {
        return reQty;
    }

    public void setReQty(int reQty) {
        this.reQty = reQty;
    }

    public int getReTotalQty() {
        return reTotalQty;
    }

    public void setReTotalQty(int reTotalQty) {
        this.reTotalQty = reTotalQty;
    }

    public int isCounted() {
        return isCounted;
    }

    public void setCounted(int counted) {
        isCounted = counted;
    }

    public boolean isOriginScanned() {
        return originScanned;
    }

    public void setOriginScanned(boolean originScanned) {
        this.originScanned = originScanned;
    }
}
