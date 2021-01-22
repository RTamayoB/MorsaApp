package com.example.morsaapp.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;

import com.example.morsaapp.Definable;
import com.example.morsaapp.R;
import com.example.morsaapp.datamodel.ScanIssuesDataModel;

import java.util.ArrayList;

public class ScanIssuesAdapter extends BaseAdapter {

    private ArrayList<ScanIssuesDataModel> dataset;
    private Context mContext;
    private Definable definable;
    public ScanIssuesAdapter(ArrayList<ScanIssuesDataModel> dataset, Context mContext, Definable definable) {
        this.dataset = dataset;
        this.mContext = mContext;
        this.definable = definable;
    }

    @Override
    public int getCount() {
        return dataset.size();
    }

    @Override
    public Object getItem(int position) {
        return dataset.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ScanIssuesDataModel item = (ScanIssuesDataModel) getItem(position);


        convertView = LayoutInflater.from(mContext).inflate(R.layout.issues_scan_popup_item, null);

        Button issueBtn = convertView.findViewById(R.id.incidencie_btn);
        issueBtn.setText(item.scanIssueName);

        issueBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("Scanned Issues Adapter", Integer.toString(item.scanIssueId));
                definable.setScannedIssue(item.scanIssueId);

            }
        });

        return convertView;
    }
}
