package com.example.morsaapp.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.example.morsaapp.R;
import com.example.morsaapp.datamodel.RoutesDataModel;

import java.util.ArrayList;

public class RoutesAdapter extends BaseAdapter implements Filterable {


    private ArrayList<RoutesDataModel> dataSet;
    private ArrayList<RoutesDataModel> dataSetFull;
    private Context mContext;
    public RoutesAdapter(Context context, ArrayList<RoutesDataModel> data) {
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
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        RoutesDataModel item = (RoutesDataModel) getItem(position);

        convertView = LayoutInflater.from(mContext).inflate(R.layout.routes_item, null);

        TextView routeName = convertView.findViewById(R.id.route_name_txt);

        routeName.setText(item.getRouteName());

        return convertView;
    }

    @Override
    public Filter getFilter() {
        return mFilter;
    }

    private Filter mFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            ArrayList<RoutesDataModel> filteredList = new ArrayList<>();

            if(constraint == null || constraint.length() == 0){
                filteredList.addAll(dataSetFull);
            }else{
                String filterPattern = constraint.toString().toLowerCase().trim();

                for (RoutesDataModel item: dataSetFull) {
                    if (item.routeName.toLowerCase().contains(filterPattern)){
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

