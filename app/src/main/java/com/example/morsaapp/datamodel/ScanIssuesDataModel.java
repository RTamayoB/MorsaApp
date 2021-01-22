package com.example.morsaapp.datamodel;

public class ScanIssuesDataModel {
    public String scanIssueName;
    public int scanIssueId;
    public int isChecked;

    public int getScanIssueId() {
        return scanIssueId;
    }

    public void setScanIssueId(int scanIssueId) {
        this.scanIssueId = scanIssueId;
    }

    public String getScanIssueName() {
        return scanIssueName;
    }

    public void setScanIssueName(String scanIssueName) {
        this.scanIssueName = scanIssueName;
    }

    public int getIsChecked() {
        return isChecked;
    }

    public void setIsChecked(int isChecked) {
        this.isChecked = isChecked;
    }
}
