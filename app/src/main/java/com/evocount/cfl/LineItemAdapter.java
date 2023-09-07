package com.evocount.cfl;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.evolabs.haco.count.fpstock.R;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;

public class LineItemAdapter extends BaseAdapter {

    private final Context mContext;
    public ArrayList<LineItem> lineItems;
    public int currentPage = 0;

    public LineItemAdapter(Context context) {
        this.mContext = context;
        lineItems = new ArrayList<LineItem>();
        int currentPage = 0;
    }

    public int getCount() {
        return lineItems.size();
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
        final LineItem item = lineItems.get(position);

        // 2
        if (convertView == null) {
            final LayoutInflater layoutInflater = LayoutInflater.from(mContext);
            convertView = layoutInflater.inflate(R.layout.linearlayout_lineitem, null);
        }

        // 3
        final TextView countTextView = (TextView)convertView.findViewById(R.id.textview_q_cnt);
        final TextView codeTextView = (TextView)convertView.findViewById(R.id.textview_q_code);
        final TextView nameTextView = (TextView)convertView.findViewById(R.id.textview_q_name);
        final TextView quantityTextView = (TextView)convertView.findViewById(R.id.textview_q_quantity);
        final TextView descTextView = (TextView)convertView.findViewById(R.id.textview_q_desc);

        // 4
        NumberFormat formatter = new DecimalFormat("0,000.00");

        countTextView.setText(String.valueOf(position+1+(currentPage*20)));
        codeTextView.setText(item.code);
        nameTextView.setText(item.name);
        quantityTextView.setText(String.valueOf(item.quantity));
        descTextView.setText(item.description);

        return convertView;
    }
}