package com.example.morsaapp.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.morsaapp.Definable;
import com.example.morsaapp.datamodel.OrderRevisionDataModel;
import com.example.morsaapp.R;

import java.util.ArrayList;

public class OrderRevisionAdapter extends BaseAdapter {

    public ArrayList<OrderRevisionDataModel> dataSet;
    private Context mContext;
    private Definable x;

    public OrderRevisionAdapter(Context context, ArrayList<OrderRevisionDataModel> data, Definable x) {
        this.mContext = context;
        this.dataSet = data;
        this.x = x;
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

    public View getView(final int position, View convertView, ViewGroup parent) {
        final OrderRevisionDataModel item = (OrderRevisionDataModel) getItem(position);


        convertView = LayoutInflater.from(mContext).inflate(R.layout.order_revision_item, null);


        TextView object = convertView.findViewById(R.id.object_txt);
        TextView qty = convertView.findViewById(R.id.qty_txt);

        object.setText(item.getProductName());
        qty.setText(item.getRevisionQty()+" / "+ item.getQty());


        return convertView;
    }
}
