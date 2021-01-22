package com.example.morsaapp.adapter;

import android.app.Activity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.TextView;

import com.example.morsaapp.Definable;
import com.example.morsaapp.datamodel.IssuesPopupDataModel;
import com.example.morsaapp.R;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class IssuesPopupAdapter extends BaseAdapter {

    private ArrayList<IssuesPopupDataModel> dataSet;
    private Activity mContext;
    private Definable definable;
    private Integer pickingId;
    private Integer moveId;
    public IssuesPopupAdapter(Activity mContext, ArrayList<IssuesPopupDataModel> dataSet, Definable definable) {
        this.mContext = mContext;
        this.dataSet = dataSet;
        this.definable=definable;
    }

    public Integer getPickingId() {
        return pickingId;
    }

    public void setPickingId(Integer pickingId) {
        this.pickingId = pickingId;
    }

    public Integer getMoveId() {
        return moveId;
    }

    public void setMoveId(Integer moveId) {
        this.moveId = moveId;
    }

    public void setDataSet(ArrayList<IssuesPopupDataModel> dataSet) {
        this.dataSet = dataSet;
    }

    public ArrayList<IssuesPopupDataModel>getDataSet(){
        return dataSet;
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
    public View getView(final int position, View convertView, final ViewGroup parent) {

        final IssuesPopupDataModel item = (IssuesPopupDataModel) getItem(position);
        convertView = LayoutInflater.from(mContext).inflate(R.layout.issues_popup_item, null);



        TextView incid_type = convertView.findViewById(R.id.incid_type_lbl);
        final EditText number = convertView.findViewById(R.id.number);
        number.setText(item.number.toString());
        number.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if((event.getAction() == event.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)){
                    EditText editText = (EditText) v;
                    String editTextString = editText.getText().toString();
                    definable.setNumber(Integer.parseInt(editTextString), item);
                    return true;
                }
                return false;
            }
        });
        incid_type.setText(item.incid_type);
        convertView.findViewById(R.id.plus_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                definable.alterCount((IssuesPopupDataModel)getItem(position),position,true);
            }
        });
        convertView.findViewById(R.id.minus_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                definable.alterCount((IssuesPopupDataModel)getItem(position),position,false);
            }
        });
        return convertView;
    }

    public void setData(@Nullable ArrayList<IssuesPopupDataModel> arrayListToSet) {
        this.dataSet=arrayListToSet;
    }
}
