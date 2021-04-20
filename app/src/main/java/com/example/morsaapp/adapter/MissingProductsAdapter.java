package com.example.morsaapp.adapter;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.morsaapp.datamodel.MissingProductsDatamodel;
import com.example.morsaapp.R;

import java.util.ArrayList;

public class MissingProductsAdapter extends BaseAdapter {

    private ArrayList<MissingProductsDatamodel> dataSet;
    private Context mContext;

    public MissingProductsAdapter(ArrayList<MissingProductsDatamodel> dataSet, Context mContext) {
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
        final MissingProductsDatamodel item = (MissingProductsDatamodel) getItem(position);

        convertView = LayoutInflater.from(mContext).inflate(R.layout.no_product_item, null);

        final CheckBox productCheck = convertView.findViewById(R.id.mp_name_txt);
        productCheck.setText(item.getName());
        final EditText qty = convertView.findViewById(R.id.mp_qty_edt);
        qty.setText(item.missingQty.toString());

        productCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    item.isChecked = true;
                    item.missingQty = Integer.parseInt(qty.getText().toString());
                    notifyDataSetChanged();
                }
                else{
                    item.isChecked = false;
                    notifyDataSetChanged();
                }
            }
        });

        TextWatcher textWatcher = new TextWatcher() {

            public void afterTextChanged(Editable s) {
                try {
                    item.missingQty = Integer.parseInt(qty.getText().toString());
                    qty.setText(item.missingQty);
                }catch (Exception e){

                }
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    item.missingQty = Integer.parseInt(qty.getText().toString());
                    qty.setText(item.missingQty);
                }catch (Exception e){

                }
            }
        };

        qty.addTextChangedListener(textWatcher);


        if(item.isChecked){
            productCheck.setChecked(true);
        }

        return convertView;
    }
}
