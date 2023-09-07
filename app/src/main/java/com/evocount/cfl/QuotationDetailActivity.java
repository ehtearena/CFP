package com.evocount.cfl;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.evolabs.haco.count.fpstock.R;

import java.text.SimpleDateFormat;
import java.util.Date;

public class QuotationDetailActivity extends AppCompatActivity {

    ProgressDialog p;
    QuotationsAdapter quotationsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        SharedPreferences prf = getSharedPreferences("user_details",MODE_PRIVATE);
        int vatpercent = prf.getInt("vatpercent", -1);

        this.quotationsAdapter = new QuotationsAdapter(QuotationDetailActivity.this, vatpercent);

        setContentView(R.layout.activity_quotation_detail);

        Bundle b = getIntent().getExtras();
        if (b != null) {
            quotationsAdapter.quotation.lineItems = b.getParcelableArrayList("items");
            quotationsAdapter.quotation.datetime = b.getLong("datetime");
            quotationsAdapter.quotation.mode = b.getString("mode");
            quotationsAdapter.quotation.warehouseOne = b.getString("whs1");
            quotationsAdapter.quotation.warehouseTwo = b.getString("whs2");
            quotationsAdapter.quotation.warehouseOneIntent = b.getString("whs1i");
            quotationsAdapter.quotation.warehouseTwoIntent = b.getString("whs2i");
            quotationsAdapter.quotation.sent = b.getBoolean("sent");
            quotationsAdapter.quotation.supplier = b.getString("sent");
            quotationsAdapter.quotation.category = b.getString("category");
            quotationsAdapter.quotation.related_to = b.getInt("related_to");
            quotationsAdapter.quotation.stockTakeType = b.getString("stockTakeType");
            quotationsAdapter.quotation.stockTakeId = b.getInt("stock_take_id");

            if (b.getBoolean("canEdit") == true && 1==2)
            {
                Button btnEdit = (Button) findViewById(R.id.btn_qd_edit);
                btnEdit.setVisibility(View.VISIBLE);
                btnEdit.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        Intent intent = new Intent(QuotationDetailActivity.this, QuotationAddActivity.class);

                        Bundle b = new Bundle();
                        b.putParcelableArrayList("lineitems", quotationsAdapter.quotation.lineItems);
                        b.putLong("datetime", quotationsAdapter.quotation.datetime);
                        b.putString("mode", quotationsAdapter.quotation.mode);
                        b.putString("warehouseone", quotationsAdapter.quotation.warehouseOne);
                        b.putString("warehousetwo", quotationsAdapter.quotation.warehouseTwo);
                        b.putString("warehouseonei", quotationsAdapter.quotation.warehouseOneIntent);
                        b.putString("warehousetwoi", quotationsAdapter.quotation.warehouseTwoIntent);
                        b.putBoolean("sent", quotationsAdapter.quotation.sent);
                        b.putString("supplier", quotationsAdapter.quotation.supplier);
                        b.putString("category", quotationsAdapter.quotation.category);
                        b.putInt("related_to", quotationsAdapter.quotation.related_to);
                        b.putString("stockTakeType", quotationsAdapter.quotation.stockTakeType);
                        b.putInt("stock_take_id", quotationsAdapter.quotation.stockTakeId);

                        intent.putExtras(b);
                        startActivity(intent);

                        finish();
                    }
                });
            }
        }

        ListView listView = (ListView) findViewById(R.id.list_view_items);

        LayoutInflater inflater = getLayoutInflater();
        ViewGroup header = (ViewGroup) inflater.inflate(R.layout.header_quotation, listView, false);
        listView.addHeaderView(header, null, false);

        LayoutInflater inflaterFooter = getLayoutInflater();

        listView.setAdapter(quotationsAdapter);

        TextView qdmode = (TextView) findViewById(R.id.txtqmode);
        qdmode.setText(String.valueOf(quotationsAdapter.quotation.mode));

        TextView qwarehouses = (TextView) findViewById(R.id.txtqwarehouses);
        if (quotationsAdapter.quotation.mode.equals("add"))  qwarehouses.setText(String.valueOf(quotationsAdapter.quotation.warehouseOne));
        if (quotationsAdapter.quotation.mode.equals("out"))  qwarehouses.setText(quotationsAdapter.quotation.warehouseOne + " to " + quotationsAdapter.quotation.warehouseTwo);
        if (quotationsAdapter.quotation.mode.equals("in"))  qwarehouses.setText(quotationsAdapter.quotation.warehouseOne + " from " + quotationsAdapter.quotation.warehouseTwo);
        if (quotationsAdapter.quotation.mode.equals("stock"))  qwarehouses.setText(String.valueOf(quotationsAdapter.quotation.warehouseOne));

        TextView qddate = (TextView) findViewById(R.id.txtqddate);
        qddate.setText(String.valueOf(String.valueOf(new SimpleDateFormat("dd MMM yy").format(new Date(quotationsAdapter.quotation.datetime)))));

        quotationsAdapter.notifyDataSetChanged();

        Button btnBack = (Button) findViewById(R.id.btn_qd_back);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }
}
