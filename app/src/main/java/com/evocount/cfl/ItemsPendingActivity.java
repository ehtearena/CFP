package com.evocount.cfl;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.evolabs.haco.count.fpstock.R;
import com.toptoche.searchablespinnerlibrary.SearchableSpinner;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class ItemsPendingActivity extends CommonActivity implements View.OnClickListener, OnInitListener
{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_quotation_add);

        this.quotationsAdapter = new QuotationsAdapter(ItemsPendingActivity.this, 0);

        ///---------

        Bundle b = getIntent().getExtras();
        if(b != null)
        {
               ArrayList<LineItem> pendingitems = b.getParcelableArrayList("listitems");

               this.quotationsAdapter.quotation.lineItems = pendingitems;
        }

        ///---------

        ListView listView1 = (ListView)findViewById(R.id.list_view_items1);

        LayoutInflater inflater1 = getLayoutInflater();
        ViewGroup header1 = (ViewGroup)inflater1.inflate(R.layout.header_quotation, listView1, false);
        listView1.addHeaderView(header1, null, false);

        listView1.setAdapter(quotationsAdapter);

        ListView listView = (ListView)findViewById(R.id.list_view_items);

        listView.setAdapter(this.quotationsAdapter);

        this.quotationsAdapter.notifyDataSetChanged();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView parent, View view, int position, long id) {
                processItemSelected( quotationsAdapter.quotation.lineItems.get(position), "Update item", "", "QTY", position-1);
            }
        });


    }

    protected void processItemSelected(LineItem li, String title, String existingItem, String dtype, int position) {

        AlertDialog.Builder builder = new AlertDialog.Builder(ItemsPendingActivity.this);
        builder.setTitle(title);

        final TextView tx = new TextView(ItemsPendingActivity.this);
        tx.setText("Quantity: ");
        final EditText input = new EditText(ItemsPendingActivity.this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(li.curQuantity));

        LinearLayout layout = new LinearLayout(ItemsPendingActivity.this);
        layout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 0, 60, 0);

        layout.addView(tx);
        layout.addView(input);

        builder.setView(layout);

        builder.setPositiveButton("Update", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                //add items to list
                LineItem li = quotationsAdapter.quotation.lineItems.get(position+1);
                li.curQuantity = Integer.parseInt(input.getText().toString());
                ((MyApplication) getApplication()).takeLi.add(li);
                quotationsAdapter.quotation.lineItems.remove(position+1);
                quotationsAdapter.notifyDataSetChanged();

                Toast.makeText(ItemsPendingActivity.this, "Item added to stock take list.", Toast.LENGTH_LONG).show();

            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }


    @Override
    public void onInit(int i) {

    }

    @Override
    public void onClick(View view) {

    }
}
