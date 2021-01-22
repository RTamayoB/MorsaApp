package com.example.morsaapp.adapter;

import android.content.Context;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.TextView;

import com.example.morsaapp.R;
import com.example.morsaapp.datamodel.TransferDataModel;

import java.util.ArrayList;

public class TransferAdapter extends BaseAdapter {

    private ArrayList<TransferDataModel> dataSet;
    private Context mContext;

    public TransferAdapter(ArrayList<TransferDataModel> dataSet, Context mContext) {
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
        final TransferDataModel item = (TransferDataModel) getItem(position);
        convertView = LayoutInflater.from(mContext).inflate(R.layout.transfer_item, null);

        TextView productName = convertView.findViewById(R.id.product_name_txt);
        final EditText productQty = convertView.findViewById(R.id.product_qty_edt);
        productQty.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    String val = productQty.getText().toString();
                    item.setQtyToTransfer(Integer.parseInt(val));
                }
                return false;
            }
        });
        productQty.setText(Integer.toString(item.getQtyToTransfer()));
        productName.setText(item.transferName);
        return convertView;
    }
}
