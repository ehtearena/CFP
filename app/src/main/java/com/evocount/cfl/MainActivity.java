package com.evocount.cfl;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.evolabs.haco.count.fpstock.R;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class MainActivity extends CommonActivity {

    ProgressDialog p;

    public void refreshTasks()
    {
        HashMap<String, Quotation> quotations = ((MyApplication)getApplication()).retrieveSerializedQuotations();

        FrameLayout quoteFrame = findViewById(R.id.quoteframe);
        TextView quoteNumber = findViewById(R.id.quotenumber);

        if (quotations.size() > 0)
        {
            quoteNumber.setText(String.valueOf(quotations.size()));
        }
        else
        {
            quoteFrame.setVisibility(View.VISIBLE);
        }

        for (String dt : quotations.keySet())
        {
            Log.d("Stored quotation", dt);
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        refreshTasks();
    }

    @Override
    public void onRestart()
    {
        super.onRestart();
        refreshTasks();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        p = new ProgressDialog(MainActivity.this);
        p.setIndeterminate(false);
        p.setCancelable(false);

        SharedPreferences pref = getSharedPreferences("user_details",MODE_PRIVATE);
        TextView textViewName = findViewById(R.id.textViewName);
        textViewName.setText("Logged in: " + pref.getString("loggedName", ""));

        FloatingActionButton quote = findViewById(R.id.quotebutton);
        quote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, SavedQuotationActivity.class);
                startActivity(intent);
            }
        });

        updateCategory();


    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu (Menu menu) {
//            menu.getItem(0).setEnabled(false);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        SharedPreferences pref = getSharedPreferences("user_details",MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        switch (item.getItemId()) {
            case R.id.stock_take:
                editor.putString("mode", "stock");
                editor.commit();

                ((MyApplication) getApplication()).takeAdaptor.quotation.lineItems.clear();

                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(MainActivity.this);

                builder.setTitle("Stock take:");

                setTitle("Stock management");

                LinearLayout layout = new LinearLayout(MainActivity.this);
                layout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                layout.setOrientation(LinearLayout.VERTICAL);
                layout.setPadding(60, 0, 60, 0);

                p.setMessage("Loading stock takes");
                p.show();

                String request = "241|<EOF>" ;

                ((MyApplication)getApplication()).setResponse("");
                ((MyApplication)getApplication()).setRequest(request);
                ((MyApplication)getApplication()).startCT();

                Timer t = new Timer();
                t.scheduleAtFixedRate(new TimerTask(){
                    @Override
                    public void run(){
                        if (((MyApplication)getApplication()).getResponse().equals("error"))
                        {
                            t.cancel();

                            runOnUiThread(new Runnable() {

                                @Override
                                public void run() {

                                    try {

                                        p.dismiss();

                                        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(MainActivity.this);
                                        builder.setTitle("Please check connectivity and try again.");
                                        builder.setNeutralButton("Okay", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                            }
                                        });

                                        builder.show();
                                    } catch (Exception ep) {
                                        ep.printStackTrace();
                                    }
                                }
                            });
                        } else
                        if (!((MyApplication)getApplication()).getResponse().equals("")) //Save Quotation
                        {
                            t.cancel();

                            runOnUiThread(new Runnable() {

                                @Override
                                public void run()
                                {
                                    try
                                    {
                                        ArrayList<StockTakes> stocktakes = new ArrayList<StockTakes>();

                                        p.dismiss();

                                        String resp = ((MyApplication)getApplication()).getResponse().replace("&"," AND ");

                                        InputSource is = new InputSource();
                                        is.setCharacterStream(new StringReader(resp));

                                        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                                        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                                        Document doc = dBuilder.parse(is);

                                        Element element = doc.getDocumentElement();
                                        element.normalize();

                                        NodeList nList = doc.getElementsByTagName("row");

                                        for (int i = 0; i < nList.getLength(); i++) {

                                            Node node = nList.item(i);
                                            if (node.getNodeType() == Node.ELEMENT_NODE) {
                                                Element element2 = (Element) node;

                                                Element id = (Element) element2.getElementsByTagName("id").item(0);
                                                Element name = (Element) element2.getElementsByTagName("name").item(0);

                                                if (element2.getTextContent() != null) {
                                                    StockTakes st = new StockTakes(Integer.parseInt(id.getTextContent().toString()), name.getTextContent().toString());
                                                    stocktakes.add(st);
                                                }
                                            }
                                        }


                                        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(MainActivity.this);
                                        builder.setTitle("Stock takes");
                                        if (stocktakes.size() == 0)
                                        {
                                            LinearLayout layout = new LinearLayout(MainActivity.this);
                                            layout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                                            layout.setOrientation(LinearLayout.VERTICAL);
                                            layout.setPadding(60, 60, 60, 0);

                                            final TextView tx = new TextView(MainActivity.this);
                                            tx.setText("There are no stock takes scheduled" );

                                            layout.addView(tx);

                                            builder.setView(layout);

                                            builder.setPositiveButton("Okay", new DialogInterface.OnClickListener() {

                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                }
                                            });
                                        }
                                        else
                                        {
                                            LinearLayout layout = new LinearLayout(MainActivity.this);
                                            layout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                                            layout.setOrientation(LinearLayout.VERTICAL);
                                            layout.setPadding(60, 0, 60, 0);

                                            final Spinner whsSpinner = new Spinner(MainActivity.this);

                                            ArrayAdapter<StockTakes> whsAdapter = new ArrayAdapter<StockTakes>(MainActivity.this, android.R.layout.simple_spinner_dropdown_item, stocktakes);
                                            whsSpinner.setAdapter(whsAdapter);

                                            final TextView tx = new TextView(MainActivity.this);
                                            tx.setText("Select stock take");

                                            layout.addView(tx);
                                            layout.addView(whsSpinner);

                                            builder.setView(layout);

                                            builder.setPositiveButton("Okay", new DialogInterface.OnClickListener() {

                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {

                                                    StockTakes st = (StockTakes) whsSpinner.getSelectedItem();

                                                    ((MyApplication) getApplication()).stockTakeId = st.getId();

                                                    Intent intent3 = new Intent(MainActivity.this, QuotationAddActivity.class);
                                                    startActivityForResult(intent3, 1);


                                                }
                                            });

                                        }
                                        builder.show();

                                    }
                                    catch (Exception e)
                                    {
                                        e.printStackTrace();
                                    }
                                }
                            });
                        }
                    }
                },500,1000);



                return true;
            case R.id.logout:
                editor.clear();
                editor.commit();
                finish();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private boolean checkAndRequestPermissions() {
        int permissionCamera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);

        int permissionReadStorage = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        int permissionWriteStorage = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        List<String> listPermissionsNeeded = new ArrayList<>();
        if (permissionCamera != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.CAMERA);
        }
        if (permissionReadStorage != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if (permissionWriteStorage != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), 402);
            return false;
        }
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1) {
            updateUI();
        }
    }

    public void updateUI()
    {
        TextView quoteNumber = findViewById(R.id.quotenumber);
        int qn = Integer.parseInt(quoteNumber.getText().toString());

        //Timer t = new Timer();
        //t.scheduleAtFixedRate(new TimerTask(){
       //     @Override
        //    public void run(){


                //Quotenumbers
                if (((MyApplication)getApplication()).retrieveSerializedQuotations().size() != qn)
                {
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {

                            try
                            {
                                refreshTasks();
                            }
                            catch (Exception e)
                            {
                                e.printStackTrace();
                            }
                        }
                    });
                }
         //       else
         //       {
          //          t.cancel();
         //       }
        //    }
       // },1000,1000);

    }
}
