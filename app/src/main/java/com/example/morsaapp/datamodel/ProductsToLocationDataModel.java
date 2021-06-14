package com.example.morsaapp.datamodel;

public class ProductsToLocationDataModel implements Comparable<ProductsToLocationDataModel> {
    public int id;
    public String origin;
    public boolean originScanned;
    public String productId;
    public String stockMoveName;
    public String location;
    public boolean isChecked;
    public int qty;
    public int total_qty;
    public int lineScanned;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public int getQty() {
        return qty;
    }

    public void setQty(int qty) {
        this.qty = qty;
    }

    public String getStockMoveName() {
        return stockMoveName;
    }

    public void setStockMoveName(String stockMoveName) {
        this.stockMoveName = stockMoveName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public int getTotal_qty() {
        return total_qty;
    }

    public void setTotal_qty(int total_qty) {
        this.total_qty = total_qty;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public boolean isOriginScanned() {
        return originScanned;
    }

    public void setOriginScanned(boolean originScanned) {
        this.originScanned = originScanned;
    }

    public int isLineScanned() {
        return lineScanned;
    }

    public void setLineScanned(int lineScanned) {
        this.lineScanned = lineScanned;
    }

    @Override
    public int compareTo(ProductsToLocationDataModel o) {
        String loc1 = origin.replace("-","");
        String loc2 = o.origin.replace("-","");
        if(Integer.parseInt(loc1) > Integer.parseInt(loc2)){
            return 1;
        }
        else if(Integer.parseInt(loc1) < Integer.parseInt(loc2)){
            return -1;
        }
        else {
            return 0;
        }
    }
}
