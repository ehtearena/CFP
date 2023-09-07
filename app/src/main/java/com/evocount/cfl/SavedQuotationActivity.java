package com.evocount.cfl;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import com.evolabs.haco.count.fpstock.R;

import java.io.IOException;
import java.util.HashMap;

public class SavedQuotationActivity extends AppCompatActivity {

    QuotationLookupAdapter adapter;
    private ListView itemList;

    @Override
    public void onResume()
    {
        super.onResume();
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onRestart()
    {
        super.onRestart();
        adapter.notifyDataSetChanged();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_quotations);


        Button btnCancel = (Button) findViewById(R.id.btnqlLookupItem2);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        itemList = (ListView) findViewById(R.id.list_sq_lookup_items);

        LayoutInflater inflater = getLayoutInflater();
        ViewGroup header = (ViewGroup)inflater.inflate(R.layout.linearlayout_quotation_lookup, itemList, false);
        itemList.addHeaderView(header, null, false);

        adapter = new QuotationLookupAdapter(SavedQuotationActivity.this);

        itemList.setAdapter(adapter);

        HashMap<String, Quotation> quotations = ((MyApplication)getApplication()).retrieveSerializedQuotations();
        if (quotations.size() > 0)
        {
            for (String qd: quotations.keySet())
            {
                Log.d("Adding session", qd);
                adapter.quotations.add(quotations.get(qd));
            }
        }

        adapter.notifyDataSetChanged();

        itemList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView parent, View view, int position, long id) {
                Quotation quotation =  adapter.quotations.get(position-1);

                Intent intent = new Intent(SavedQuotationActivity.this, QuotationDetailActivity.class);

                Bundle b = new Bundle();
                b.putParcelableArrayList("items", quotation.lineItems);
                b.putLong("datetime", quotation.datetime);
                b.putString("mode", quotation.mode);
                b.putString("whs1", quotation.warehouseOne);
                b.putString("whs2", quotation.warehouseTwo);
                b.putString("whs1i", quotation.warehouseOneIntent);
                b.putString("whs2i", quotation.warehouseTwoIntent);
                b.putBoolean("sent", quotation.sent);
                b.putString("supplier", quotation.supplier);
                b.putString("category", quotation.category);
                b.putString("stockTakeType", quotation.stockTakeType);
                b.putInt("related_to", quotation.related_to);
                b.putInt("stock_take_id", quotation.stockTakeId);

                if ((quotation.mode.equals("in")|| quotation.mode.equals("out")) && quotation.sent == true )
                {
                    b.putBoolean("canEdit", false);
                }
                else
                {
                    b.putBoolean("canEdit", true);
                }
                intent.putExtras(b);
                startActivity(intent);
//                finish();
            }
        });

        itemList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView parent, View view, int position, long id) {

                AlertDialog.Builder builder = new AlertDialog.Builder(SavedQuotationActivity.this);
                builder.setTitle("Delete saved item?");
                builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        Quotation removalQuotation = adapter.quotations.get(position-1);
                        try {
                            ((MyApplication)getApplication()).removeSerializedQuotation(removalQuotation);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        adapter.quotations.remove(position-1);
                        adapter.notifyDataSetChanged();

                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                builder.show();

                return true;
            }
        });

    }
}