package com.evocount.cfl;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.evolabs.haco.count.fpstock.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class QuotationLookupAdapter extends BaseAdapter {

    private final Context mContext;
    ArrayList<Quotation> quotations = new ArrayList<Quotation>();

    public QuotationLookupAdapter(Context context) {
        this.mContext = context;
        quotations = new ArrayList<Quotation>();
    }

    public int getCount() {
        return quotations.size();
    }

    public long getItemId(int position) {
        return 0;
    }

    public Object getItem(int position) {
        return null;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        // 1
        final Quotation quotation = quotations.get(position);

        // 2
        if (convertView == null) {
            final LayoutInflater layoutInflater = LayoutInflater.from(mContext);
            convertView = layoutInflater.inflate(R.layout.linearlayout_quotation_lookup, null);
        }

        // 3
        final TextView countTextView = (TextView)convertView.findViewById(R.id.textview_ql_cnt);
        final TextView whs1TextView = (TextView)convertView.findViewById(R.id.textview_ql_whs1);
        final TextView whs2TextView = (TextView)convertView.findViewById(R.id.textview_ql_whs2);
        final TextView itemCountTextView = (TextView)convertView.findViewById(R.id.textview_ql_item_count);
        final TextView modeTextView = (TextView)convertView.findViewById(R.id.textview_ql_mode);
        final TextView datetimeTextView = (TextView)convertView.findViewById(R.id.textview_ql_datetime);

        // 4

        String sentStatus = "";
        if (quotation.sent)
        {
            sentStatus = " (S)";
        }

        countTextView.setText(String.valueOf(position+1) + sentStatus);
        itemCountTextView.setText(String.valueOf(quotation.lineItems.size()));
        whs1TextView.setText(quotation.warehouseOneIntent);
        whs2TextView.setText(quotation.warehouseTwoIntent);

        String category = "";
        if (!quotation.category.equals(""))
        {
            category = " ("+quotation.category+")";
        }

        modeTextView.setText(quotation.mode + category);


        Date date = new Date();
        datetimeTextView.setText(String.valueOf(new SimpleDateFormat("dd MMM yy").format(new Date(quotation.datetime))));

        return convertView;
    }
}