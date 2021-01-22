package com.example.morsaapp.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.morsaapp.R;
import com.example.morsaapp.datamodel.ReStockDataModel;

import java.util.ArrayList;

public class ReStockAdapter extends BaseAdapter {

    private ArrayList<ReStockDataModel> dataSet;
    private Context mContext;

    public ReStockAdapter(ArrayList<ReStockDataModel> dataSet, Context mContext) {
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
        ReStockDataModel item = (ReStockDataModel)getItem(position);

        convertView = LayoutInflater.from(mContext).inflate(R.layout.re_stock_item, null);

        TextView product = convertView.findViewById(R.id.re_stock_product_txt);
        TextView origin = convertView.findViewById(R.id.re_stock_origin_txt);
        TextView destiny = convertView.findViewById(R.id.re_stock_destiny_txt);
        TextView qty = convertView.findViewById(R.id.re_stock_qty_txt);

        product.setText(item.reProduct);
        origin.setText(item.reOrigin);
        destiny.setText(item.reDestiny);
        qty.setText(item.reQty+"/"+item.reTotalQty);

        if(item.isCounted == 1){
            product.setTextColor(Color.WHITE);
            origin.setTextColor(Color.WHITE);
            destiny.setTextColor(Color.WHITE);
            qty.setTextColor(Color.WHITE);

            product.setBackgroundColor(Color.parseColor("#ffa500"));
            origin.setBackgroundColor(Color.parseColor("#ffa500"));
            destiny.setBackgroundColor(Color.parseColor("#ffa500"));
            qty.setBackgroundColor(Color.parseColor("#ffa500"));
        }
        else if(item.isCounted == 2){
            product.setTextColor(Color.WHITE);
            origin.setTextColor(Color.WHITE);
            destiny.setTextColor(Color.WHITE);
            qty.setTextColor(Color.WHITE);

            product.setBackgroundColor(Color.parseColor("#008f39"));
            origin.setBackgroundColor(Color.parseColor("#008f39"));
            destiny.setBackgroundColor(Color.parseColor("#008f39"));
            qty.setBackgroundColor(Color.parseColor("#008f39"));
        }
        return convertView;
    }
}
