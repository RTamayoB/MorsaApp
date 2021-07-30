package com.example.morsaapp.adapter;

import android.content.Context;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
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

        if (item.relabel.equals("true")){
            object.setBackgroundColor(Color.parseColor("#ffa500"));
            String original = object.getText().toString();
            String relabel = " - Reetiquetado";
            String myText = original + relabel;
            Spannable span = new SpannableString(myText);
            span.setSpan(new ForegroundColorSpan(Color.BLACK), 0, original.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            span.setSpan(new ForegroundColorSpan(Color.RED), original.length(), myText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            object.setText(span, TextView.BufferType.SPANNABLE);
        }

        //if 1 colr qty green
        if(item.lineScanned == 1){
            qty.setBackgroundColor(Color.parseColor("#008f39"));
            qty.setTextColor(Color.WHITE);

            object.setBackgroundColor(Color.parseColor("#008f39"));
            object.setTextColor(Color.WHITE);
        }

        return convertView;
    }
}
