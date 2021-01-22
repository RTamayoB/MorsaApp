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
import com.example.morsaapp.datamodel.ReceptionDataModel;

import java.util.ArrayList;
import java.util.Iterator;

public class RevisionAdapter extends BaseAdapter implements Filterable {

    private ArrayList<ReceptionDataModel> dataSet;
    private ArrayList<ReceptionDataModel> dataSetFull;
    private Context mContext;

    public RevisionAdapter(Context context, ArrayList<ReceptionDataModel> data) {
        this.mContext = context;
        this.dataSet = data;
        this.dataSetFull = new ArrayList<>(dataSet);
        Iterator ite = data.iterator();
        while (ite.hasNext()){
            ReceptionDataModel rdm = (ReceptionDataModel) ite.next();
            if (!rdm.show){
                ite.remove();
            }
        }
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
        ReceptionDataModel item = (ReceptionDataModel) getItem(position);

        convertView = LayoutInflater.from(mContext).inflate(R.layout.revision_item, null);


        TextView num = convertView.findViewById(R.id.rev_num_txt);
        TextView date = convertView.findViewById(R.id.rev_date_txt);
        TextView time = convertView.findViewById(R.id.rev_time_txt);
        TextView box = convertView.findViewById(R.id.rev_box_txt);
        TextView partner = convertView.findViewById(R.id.rev_partner_txt);

        num.setText(item.displayName);
        date.setText(item.getDate());
        time.setText(item.time);
        box.setText(item.getBox());
        partner.setText(item.getCompany());

        return convertView;
    }

    @Override
    public Filter getFilter() {
        return mFilter;
    }

    private Filter mFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            ArrayList<ReceptionDataModel> filteredList = new ArrayList<>();

            if(constraint == null || constraint.length() == 0){
                filteredList.addAll(dataSetFull);
            }else{
                String filterPattern = constraint.toString().toLowerCase().trim();

                for (ReceptionDataModel item: dataSetFull) {
                    if (item.displayName.toLowerCase().contains(filterPattern)){
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
