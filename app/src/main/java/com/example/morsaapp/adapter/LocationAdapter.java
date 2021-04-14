package com.example.morsaapp.adapter;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.morsaapp.datamodel.LocationDataModel;
import com.example.morsaapp.R;

import java.util.ArrayList;

public class LocationAdapter extends BaseAdapter {

    private ArrayList<LocationDataModel> dataSet;
    private Activity mContext;

    public LocationAdapter(ArrayList<LocationDataModel> dataSet, Activity mContext) {
        this.dataSet = dataSet;
        this.mContext = mContext;
    }

    @Override
    public int getCount() {
        return dataSet.size();
    }

    @Override
    public Object getItem(int position) {
        return dataSet.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final LocationDataModel item = (LocationDataModel) getItem(position);
        convertView = LayoutInflater.from(mContext).inflate(R.layout.location_item, null);

        TextView productNameTxt = convertView.findViewById(R.id.product_name_txt);
        TextView productIdTxt = convertView.findViewById(R.id.product_id_txt);
        TextView locationTxt = convertView.findViewById(R.id.location_txt);

        productNameTxt.setText(item.getProductName());
        productIdTxt.setText(item.getProductId());
        locationTxt.setText(item.getLocation());

        return convertView;
    }
}
