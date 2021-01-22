package com.example.morsaapp.datamodel;

public class CountDataModel /*implements Comparable<CountDataModel>*/ {

    public Integer lineId;
    public String realLocation;
    public String location;
    public String code;
    public String theoricalQty;
    public String totalQty;
    public boolean isCounted;

    public Integer getLineId() {
        return lineId;
    }

    public void setLineId(Integer lineId) {
        this.lineId = lineId;
    }

    public String getRealLocation() {
        return realLocation;
    }

    public void setRealLocation(String realLocation) {
        this.realLocation = realLocation;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getTheoricalQty() {
        return theoricalQty;
    }

    public void setTheoricalQty(String theoricalQty) {
        this.theoricalQty = theoricalQty;
    }

    public String getTotalQty() {
        return totalQty;
    }

    public void setTotalQty(String totalQty) {
        this.totalQty = totalQty;
    }

    public boolean getIsCounted() {
        return isCounted;
    }

    public void setIsCounted(boolean isCounted) {
        this.isCounted = isCounted;
    }

    /*
    @Override
    public int compareTo(CountDataModel o) {

        String loc1 = location.replace("-","");
        String loc2 = o.location.replace("-","");

        if(Integer.parseInt(loc1) > Integer.parseInt(loc2)){
            return 1;
        }
        else if(Integer.parseInt(loc1) < Integer.parseInt(loc2)){
            return -1;
        }
        else{
            return 0;
        }
    }*/
}
