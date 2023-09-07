package com.evocount.cfl;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.evolabs.haco.count.fpstock.R;

public class QuotationsAdapter extends BaseAdapter {

    private final Context mContext;
    public Quotation quotation;
    public int vatpercent;

    public QuotationsAdapter(Context context, int vatperc) {
        this.mContext = context;
        this.quotation = new Quotation(vatperc);
        this.vatpercent = vatperc;
    }

    public int getCount() {
        return quotation.lineItems.size();
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
        final LineItem item = quotation.lineItems.get(position);

        // 2
        if (convertView == null) {
            final LayoutInflater layoutInflater = LayoutInflater.from(mContext);
            convertView = layoutInflater.inflate(R.layout.linearlayout_quotation, null);
        }

        // 3
        final TextView countTextView = (TextView)convertView.findViewById(R.id.textview_q_cnt);
        final TextView codeTextView = (TextView)convertView.findViewById(R.id.textview_q_code);
//        final TextView warehouseTextView = (TextView)convertView.findViewById(R.id.textview_q_warehouse);
        final TextView nameTextView = (TextView)convertView.findViewById(R.id.textview_q_name);
        final TextView quantityTextView = (TextView)convertView.findViewById(R.id.textview_q_quantity);
        final TextView curQuantityTextView = (TextView)convertView.findViewById(R.id.textview_q_curQuantity);
        final TextView descTextView = (TextView)convertView.findViewById(R.id.textview_q_desc);

        countTextView.setText(String.valueOf(position+1));
        codeTextView.setText(item.code);
  //      warehouseTextView.setText(item.warehouse);
        nameTextView.setText(item.barcode + " - " + item.name);
        quantityTextView.setText(String.valueOf(item.quantity));
        curQuantityTextView.setText(String.valueOf(item.curQuantity));
        descTextView.setText(item.description);

        return convertView;
    }


}