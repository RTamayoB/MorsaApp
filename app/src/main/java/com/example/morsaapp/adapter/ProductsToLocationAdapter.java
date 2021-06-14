package com.example.morsaapp.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.morsaapp.datamodel.ProductsToLocationDataModel;
import com.example.morsaapp.R;

import java.util.ArrayList;
import java.util.Collections;

public class    ProductsToLocationAdapter extends BaseAdapter {

    private ArrayList<ProductsToLocationDataModel> dataSet;
    private Context mContext;

    public ProductsToLocationAdapter(ArrayList<ProductsToLocationDataModel> dataSet, Context mContext) {
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
        ProductsToLocationDataModel item = (ProductsToLocationDataModel) getItem(position);

        convertView = LayoutInflater.from(mContext).inflate(R.layout.products_to_location_item, null);

        if (item.isChecked){
            convertView.setBackgroundColor(Color.GREEN);
        }

        TextView product = convertView.findViewById(R.id.product_txt);
        TextView location = convertView.findViewById(R.id.location_txt);
        TextView qty = convertView.findViewById(R.id.product_to_location_qty);
        TextView origin = convertView.findViewById(R.id.origin_txt);
        product.setText(item.getStockMoveName());
        location.setText(item.getLocation());
        qty.setText(item.getQty()+"/"+item.getTotal_qty());
        origin.setText(item.getOrigin());

        //Color origin if scanned
        if(item.originScanned){
            origin.setBackgroundColor(Color.CYAN);
        }

        //Color for line
        if(item.lineScanned == 1){
            product.setTextColor(Color.WHITE);
            location.setTextColor(Color.WHITE);
            qty.setTextColor(Color.WHITE);
            origin.setTextColor(Color.WHITE);

            product.setBackgroundColor(Color.parseColor("#008f39"));
            location.setBackgroundColor(Color.parseColor("#008f39"));
            qty.setBackgroundColor(Color.parseColor("#008f39"));
            origin.setBackgroundColor(Color.parseColor("#008f39"));
        }
        else if(item.lineScanned == 2){
            product.setTextColor(Color.WHITE);
            location.setTextColor(Color.WHITE);
            qty.setTextColor(Color.WHITE);
            origin.setTextColor(Color.WHITE);

            product.setBackgroundColor(Color.parseColor("#ffa500"));
            location.setBackgroundColor(Color.parseColor("#ffa500"));
            qty.setBackgroundColor(Color.parseColor("#ffa500"));
            origin.setBackgroundColor(Color.parseColor("#ffa500"));
        }
        else if(item.lineScanned == 3){
            product.setTextColor(Color.WHITE);
            location.setTextColor(Color.WHITE);
            qty.setTextColor(Color.WHITE);
            origin.setTextColor(Color.WHITE);

            product.setBackgroundColor(Color.parseColor("#ba3200"));
            location.setBackgroundColor(Color.parseColor("#ba3200"));
            qty.setBackgroundColor(Color.parseColor("#ba3200"));
            origin.setBackgroundColor(Color.parseColor("#ba3200"));
        }

        return convertView;
    }
}
