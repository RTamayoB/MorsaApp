package com.example.morsaapp.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.example.morsaapp.datamodel.CountDataModel;
import com.example.morsaapp.R;

import java.util.ArrayList;

public class CountAdapter extends BaseAdapter implements Filterable {

    private ArrayList<CountDataModel> dataSet;
    private ArrayList<CountDataModel> dataSetFull;
    private Context mContext;

    public CountAdapter(ArrayList<CountDataModel> dataSet, Context mContext) {
        this.dataSet = dataSet;
        this.mContext = mContext;
        this.dataSetFull = new ArrayList<>(dataSet);
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

        CountDataModel item = (CountDataModel)getItem(position);

        convertView = LayoutInflater.from(mContext).inflate(R.layout.count_item, null);

        TextView location = convertView.findViewById(R.id.location_txt);
        TextView code = convertView.findViewById(R.id.code_txt);
        TextView qty = convertView.findViewById(R.id.pzqty_txt);

        location.setText(item.location);
        code.setText(item.code);
        qty.setText(item.totalQty);

        if(item.isCounted){
            location.setBackgroundColor(Color.parseColor("#008f39"));
            location.setTextColor(Color.WHITE);
            code.setBackgroundColor(Color.parseColor("#008f39"));
            code.setTextColor(Color.WHITE);
            qty.setBackgroundColor(Color.parseColor("#008f39"));
            qty.setTextColor(Color.WHITE);
            //convertView.setBackgroundColor(Color.parseColor("#008f39"));
        }

        if(item.isLocation){
            location.setBackgroundColor(Color.CYAN);
        }

        if (item.isReported){
            location.setBackgroundColor(Color.parseColor("#FF8000"));
            location.setTextColor(Color.WHITE);
            code.setBackgroundColor(Color.parseColor("#FF8000"));
            code.setTextColor(Color.WHITE);
            qty.setBackgroundColor(Color.parseColor("#FF8000"));
            qty.setTextColor(Color.WHITE);
        }

        return convertView;
    }

    @Override
    public Filter getFilter() {
        return mFilter;
    }

    private Filter mFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            ArrayList<CountDataModel> filteredList = new ArrayList<>();

            if(constraint == null || constraint.length() == 0){
                filteredList.addAll(dataSetFull);
            }else{
                String filterPattern = constraint.toString().toLowerCase().trim();

                for (CountDataModel item: dataSetFull) {
                    if (item.code.toLowerCase().contains(filterPattern)){
                        filteredList.add(item);
                    }
                }
            }

            FilterResults results = new FilterResults();
            results.values = filteredList;

            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            dataSet.clear();
            dataSet.addAll((ArrayList) results.values);
            notifyDataSetChanged();
        }
    };
}
