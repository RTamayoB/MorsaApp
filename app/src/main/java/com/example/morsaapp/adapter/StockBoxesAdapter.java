package com.example.morsaapp.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.morsaapp.R;
import com.example.morsaapp.datamodel.StockBoxesDataModel;

import java.util.ArrayList;

public class StockBoxesAdapter extends BaseAdapter {

    private ArrayList<StockBoxesDataModel> dataset;
    private Context mContext;

    public StockBoxesAdapter(Context context, ArrayList<StockBoxesDataModel> data) {
        this.dataset = data;
        this.mContext = context;
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
        StockBoxesDataModel item = (StockBoxesDataModel) getItem(position);

        convertView = LayoutInflater.from(mContext).inflate(R.layout.stock_boxes_item, null);

        TextView invoicesTxt = convertView.findViewById(R.id.invoices_txt);
        TextView boxNumTxt = convertView.findViewById(R.id.boxNum_txt);

        invoicesTxt.setText(item.invoices);
        boxNumTxt.setText(item.box);

        //Color cyan if counted
        if(item.isScanned){
            convertView.setBackgroundColor(Color.CYAN);
        }

        return convertView;
    }
}
