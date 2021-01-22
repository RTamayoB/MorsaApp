package com.example.morsaapp.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.morsaapp.datamodel.InvoiceDataModel;
import com.example.morsaapp.R;

import java.util.ArrayList;

public class InvoiceAdapter extends BaseAdapter {

    private ArrayList<InvoiceDataModel> dataSet;
    private Context mContext;

    public InvoiceAdapter(ArrayList<InvoiceDataModel> dataSet, Context mContext) {
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
        InvoiceDataModel item = (InvoiceDataModel) getItem(position);

        convertView = LayoutInflater.from(mContext).inflate(R.layout.invoice_item, null);


        TextView product = convertView.findViewById(R.id.product_txt);
        TextView imports = convertView.findViewById(R.id.import_txt);

       product.setText(item.getProduct());
        imports.setText(item.getImports());

        return convertView;
    }
}
