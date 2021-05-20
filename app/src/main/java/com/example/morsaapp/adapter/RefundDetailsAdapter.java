package com.example.morsaapp.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.morsaapp.Definable;
import com.example.morsaapp.R;
import com.example.morsaapp.datamodel.OrderRevisionDataModel;
import com.example.morsaapp.datamodel.RefundDetailsDataModel;

import java.util.ArrayList;

public class RefundDetailsAdapter extends BaseAdapter {

    public ArrayList<RefundDetailsDataModel> dataSet;
    private Context mContext;

    public RefundDetailsAdapter(ArrayList<RefundDetailsDataModel> dataSet, Context mContext) {
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
        return dataSet.get(position).Id;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final RefundDetailsDataModel item = (RefundDetailsDataModel) getItem(position);

        convertView = LayoutInflater.from(mContext).inflate(R.layout.refund_item, null);

        TextView invoiceTxt = convertView.findViewById(R.id.invoice_txt);
        TextView productTxt = convertView.findViewById(R.id.product_txt);
        TextView refundsQtyTxt = convertView.findViewById(R.id.refundQty_txt);

        invoiceTxt.setText(item.name);
        productTxt.setText(item.productName);
        refundsQtyTxt.setText(item.getRevisionQty()+" / "+ item.getQty());

        return convertView;
    }
}
