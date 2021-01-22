package com.example.morsaapp;

import com.example.morsaapp.datamodel.IssuesPopupDataModel;

public interface Definable {

    public void showPopup(Integer value, int position, String productName);
    public void alterCount(IssuesPopupDataModel item, Integer position, Boolean value);
    public void setNumber(int newvalue, IssuesPopupDataModel item);
    public void setScannedIssue(int position);
}
