package com.example.morsaapp.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.example.morsaapp.datamodel.PickingDataModel;
import com.example.morsaapp.R;

import java.util.ArrayList;

public class PickingAdapter extends BaseAdapter{

    private ArrayList<PickingDataModel> dataSet;
    private ArrayList<PickingDataModel> dataSetFull;
    private Context mContext;

    public PickingAdapter(Context context, ArrayList<PickingDataModel> data) {
        this.mContext = context;
        this.dataSet = data;
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
        return Integer.parseInt(dataSet.get(position).id);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        PickingDataModel item = (PickingDataModel) getItem(position);


        if(convertView == null){
            convertView = LayoutInflater.from(mContext).inflate(R.layout.picking_item, null);


            TextView num = convertView.findViewById(R.id.picking_name_txt);
            TextView date = convertView.findViewById(R.id.picking_date_txt);
            TextView time = convertView.findViewById(R.id.picking_time_txt);
            TextView box = convertView.findViewById(R.id.picking_box_txt);

            num.setText(item.getName());
            String[] timeDate = item.date.split(" ");
            date.setText(timeDate[0]);
            time.setText(timeDate[1]);
            box.setText(item.getBox());
        }
        return convertView;
    }

    /*
    @Override
    public Filter getFilter() {
        return mFilter;
    }

    private Filter mFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            ArrayList<PickingDataModel> filteredList = new ArrayList<>();

            if(constraint == null || constraint.length() == 0){
                filteredList.addAll(dataSetFull);
            }else{
                String filterPattern = constraint.toString().toLowerCase().trim();

                for (PickingDataModel item: dataSetFull) {
                    if (item.name.toLowerCase().contains(filterPattern)){
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

    };*/
}
