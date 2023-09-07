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
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.os.Bundle;
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;

import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class QuotationAddActivity extends CommonActivity implements View.OnClickListener, OnInitListener
{
    private static final int CAMERA_REQUEST = 1888;
    private ImageView imageView;
    private static final int MY_CAMERA_PERMISSION_CODE = 100;

    private final static int INTERVAL = 1000 * 60 * 5; //2 minutes
    Handler mHandler = new Handler();
    private Menu menu;
    private int MY_DATA_CHECK_CODE = 0;
    private TextToSpeech myTTS;

    ProgressDialog p;

    private SearchableSpinner partSpinner;
    private MultiSpinnerSearch partSpinnerCat;

    int itemSelectedPosition = 0;
    static final int LOOKUP_ITEM_REQUEST = 1;
    static final int EXISTING_CLIENT_REQUEST = 2;
    static final int NEW_CLIENT_REQUEST = 3;
    boolean batteryNotified = false;

    Runnable mHandlerTask = new Runnable()
    {
        @Override
        public void run() {
            try {
                //Toast.makeText(QuotationAddActivity.this, "Auto saving", Toast.LENGTH_SHORT).show();
                if (quotationsAdapter != null && quotationsAdapter.quotation != null) ((MyApplication)getApplication()).storeSerializedQuotation(quotationsAdapter.quotation);
            } catch (IOException e) {
                e.printStackTrace();
            }

            mHandler.postDelayed(mHandlerTask, INTERVAL);
        }
    };

    void startRepeatingTask()
    {
        mHandlerTask.run();
    }

    void stopRepeatingTask()
    {
        mHandler.removeCallbacks(mHandlerTask);
    }

    public String MD5(String md5) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] array = md.digest(md5.getBytes("UTF-8"));
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < array.length; ++i) {
                sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1,3));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
        } catch(UnsupportedEncodingException ex){
        }
        return null;
    }

    @Override
    public void onBackPressed()
    {
        Intent intent = new Intent();
        setResult(RESULT_OK, intent);
        stopRepeatingTask();
        finish();
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        try
        {
            myTTS.speak("Proceed with scanning", TextToSpeech.QUEUE_FLUSH, null);

            //check list and update items.
            for(LineItem li : ((MyApplication) getApplication()).takeLi)
            {
                quotationsAdapter.quotation.lineItems.add(li);
            }

            ((MyApplication) getApplication()).takeLi.clear();
            quotationsAdapter.notifyDataSetChanged();


        }
        catch (Exception e)
        {

        }

        registerReceiver(barcodeDataReceiver, new IntentFilter(ACTION_BARCODE_DATA2));
        registerReceiver(barcodeDataReceiver, new IntentFilter(ACTION_BARCODE_DATA1));
        registerReceiver(barcodeDataReceiver, new IntentFilter(ACTION_BARCODE_DATA));
        startRepeatingTask();
        claimScanner();
    }

    @Override
    public void onDestroy()
    {
        if (quotationsAdapter.quotation.lineItems.size() > 0)
        {
            try {
                ((MyApplication)getApplication()).storeSerializedQuotation(quotationsAdapter.quotation);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        stopRepeatingTask();
        super.onDestroy();
    }

    public void onSaveInstanceState(Bundle savedState) {
        super.onSaveInstanceState(savedState);
        savedState.putParcelableArrayList("items", quotationsAdapter.quotation.lineItems);
        savedState.putString("warehouseone", quotationsAdapter.quotation.warehouseOne);
        savedState.putString("warehousetwo", quotationsAdapter.quotation.warehouseTwo);
        savedState.putString("warehouseonei", quotationsAdapter.quotation.warehouseOneIntent);
        savedState.putString("warehousetwoi", quotationsAdapter.quotation.warehouseTwoIntent);
        savedState.putString("mode", quotationsAdapter.quotation.mode);
        savedState.putBoolean("sent", quotationsAdapter.quotation.sent);
        savedState.putString("category", quotationsAdapter.quotation.category);
        savedState.putString("supplier", quotationsAdapter.quotation.supplier);
        savedState.putInt("related_to", quotationsAdapter.quotation.related_to);
        savedState.putString("stockTakeType", quotationsAdapter.quotation.stockTakeType);
        savedState.putInt("stock_take_id", quotationsAdapter.quotation.stockTakeId);
    }

    private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context ctxt, Intent intent) {
            /*
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            if (level > 11 && batteryNotified == true)
            {
                batteryNotified = false;
            }

            if (level <= 10 && batteryNotified == false)
            {
                batteryNotified = true;

                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(QuotationAddActivity.this);
                builder.setTitle("Low battery");

                LinearLayout layout2 = new LinearLayout(QuotationAddActivity.this);
                layout2.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                layout2.setOrientation(LinearLayout.VERTICAL);
                layout2.setPadding(50, 30, 50, 0);

                final TextView tv1 = new TextView(QuotationAddActivity.this);
                tv1.setText("Please save your work.");

                layout2.addView(tv1);
                builder.setView(layout2);

                builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            saveQuotation("", false);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        //remarksAndReferral("");

//                            ((MyApplication)getApplication())saveQuotation.storeSerializedQuotation(quotationsAdapter.quotation);
                    }
                });

                builder.show();

            }
            */
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.item_save, menu);
        this.menu = menu;
        return true;
    }

    public ArrayList<LineItem> determinePending()
    {
        ArrayList<LineItem> lis = new ArrayList<LineItem>();

        for (LineItem lit : ((MyApplication) getApplication()).takeAdaptor.quotation.lineItems)
        {
            boolean foundx = false;
            for (LineItem lix : quotationsAdapter.quotation.lineItems)
            {
                if (lix.code.equals(lit.code))
                {
                    lit.curQuantity = lix.curQuantity;
                    foundx= true;
                }
            }

            if (foundx == false)
            {
                lis.add(lit);
            }
        }

        return lis;
    }

    public void onItemSaveAction(MenuItem mi) {

            if (mi.getTitle().equals("View Pending"))
            {
                Intent intent = new Intent(QuotationAddActivity.this, ItemsPendingActivity.class);

                ArrayList<LineItem> lis = new ArrayList<LineItem>();
                lis = determinePending();

                myTTS.speak(lis.size() + " items pending.", TextToSpeech.QUEUE_FLUSH, null);
                Toast.makeText(QuotationAddActivity.this, "Pending: " + lis.size() + " items.", Toast.LENGTH_LONG).show();

                Bundle b = new Bundle();
                b.putParcelableArrayList("listitems", lis);
                intent.putExtras(b);
                startActivity(intent);

            }

            if (mi.getTitle().equals("Item Save"))
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(QuotationAddActivity.this);
                builder.setTitle("Save stock take");

                final TextView tx = new TextView(QuotationAddActivity.this);
                tx.setText("Name: ");
                final EditText input = new EditText(QuotationAddActivity.this);

                LinearLayout layout = new LinearLayout(QuotationAddActivity.this);
                layout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                layout.setOrientation(LinearLayout.VERTICAL);
                layout.setPadding(60, 0, 60, 0);

                layout.addView(tx);
                layout.addView(input);

                builder.setView(layout);

                builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            saveQuotation("", false, input.getText().toString());
//                    Toast.makeText(QuotationAddActivity.this, "Saved; " + ((MyApplication)getApplication()).totalsaved + " items.", Toast.LENGTH_LONG).show();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

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
    }

    protected void processItemSelected(LineItem li, String title, String existingItem, String dtype, int position) {

        AlertDialog.Builder builder = new AlertDialog.Builder(QuotationAddActivity.this);
        builder.setTitle(title);

        final TextView tx = new TextView(QuotationAddActivity.this);
        tx.setText("Quantity: ");
        final EditText input = new EditText(QuotationAddActivity.this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(li.curQuantity));

        final TextView txcomment = new TextView(QuotationAddActivity.this);
        txcomment.setText("Optional Comments: ");
        final EditText inputcomment = new EditText(QuotationAddActivity.this);

        LinearLayout layout = new LinearLayout(QuotationAddActivity.this);
        layout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 0, 60, 0);

        layout.addView(tx);
        layout.addView(input);

        layout.addView(txcomment);
        layout.addView(inputcomment);

        builder.setView(layout);

        builder.setPositiveButton("Update", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                quotationsAdapter.quotation.lineItems.get(position).curQuantity = Integer.parseInt(input.getText().toString());
                quotationsAdapter.quotation.lineItems.get(position).submitted = 0;
                quotationsAdapter.quotation.lineItems.get(position).comments = inputcomment.getText().toString();

                quotationsAdapter.notifyDataSetChanged();
/*
                try {
                  //  saveQuotation("", false);
                   // myTTS.speak(input.getText().toString(), TextToSpeech.QUEUE_FLUSH, null);


                } catch (IOException e) {
                    e.printStackTrace();
                }
*/
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startRepeatingTask();
        SharedPreferences pref = getSharedPreferences("user_details",MODE_PRIVATE);
        ((MyApplication)getApplication()).mode = pref.getString("mode" , "");

        Intent checkTTSIntent = new Intent();
        checkTTSIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkTTSIntent, MY_DATA_CHECK_CODE);

        p = new ProgressDialog(QuotationAddActivity.this);
        p.setMessage("Saving data.");
        p.setIndeterminate(false);
        p.setCancelable(false);

        boolean promptMode = true;

        SharedPreferences prf = getSharedPreferences("user_details",MODE_PRIVATE);
        int vatpercent = prf.getInt("vatpercent", -1);

        this.quotationsAdapter = new QuotationsAdapter(QuotationAddActivity.this, vatpercent);

        quotationsAdapter.quotation.stockTakeId = ((MyApplication) getApplication()).stockTakeId;

        setContentView(R.layout.activity_quotation_add);

        if (savedInstanceState != null)
        {
            quotationsAdapter.quotation.lineItems = savedInstanceState.getParcelableArrayList("items");
            quotationsAdapter.quotation.warehouseOne = savedInstanceState.getString("warehouseone");
            quotationsAdapter.quotation.warehouseTwo = savedInstanceState.getString("warehousetwo");
            quotationsAdapter.quotation.warehouseOneIntent = savedInstanceState.getString("warehouseonei");
            quotationsAdapter.quotation.warehouseTwoIntent = savedInstanceState.getString("warehousetwoi");
            quotationsAdapter.quotation.mode = savedInstanceState.getString("mode");
            quotationsAdapter.quotation.supplier = savedInstanceState.getString("supplier");
            quotationsAdapter.quotation.sent = savedInstanceState.getBoolean("sent");
            quotationsAdapter.quotation.category = savedInstanceState.getString("category");
            quotationsAdapter.quotation.related_to = savedInstanceState.getInt("related_to");
            quotationsAdapter.quotation.stockTakeType = savedInstanceState.getString("stockTakeType");
            quotationsAdapter.quotation.stockTakeId = savedInstanceState.getInt("stock_take_id");
        }

        Bundle b = getIntent().getExtras();
        if(b != null)
        {
            if (b.getLong("datetime", 0) != 0)
            {
                promptMode = false;
                quotationsAdapter.quotation.datetime = b.getLong("datetime");
                quotationsAdapter.quotation.warehouseOne = b.getString("warehouseone");
                quotationsAdapter.quotation.warehouseTwo = b.getString("warehousetwo");
                quotationsAdapter.quotation.warehouseOneIntent = b.getString("warehouseonei");
                quotationsAdapter.quotation.warehouseTwoIntent = b.getString("warehousetwoi");
                quotationsAdapter.quotation.mode = b.getString("mode");
                quotationsAdapter.quotation.sent = b.getBoolean("sent");
                quotationsAdapter.quotation.category = b.getString("category");
                quotationsAdapter.quotation.supplier = b.getString("supplier");
                quotationsAdapter.quotation.related_to = b.getInt("related_to");
                quotationsAdapter.quotation.stockTakeType = b.getString("stockTakeType");
                quotationsAdapter.quotation.stockTakeId = b.getInt("stock_take_id");

                ((MyApplication)getApplication()).mode = b.getString("mode");
                ((MyApplication)getApplication()).selectedWarehouse = b.getString("warehouseone");
                ((MyApplication)getApplication()).secondWarehouse = b.getString("warehousetwo");

                String barTitle = "Stock management";
                if (((MyApplication)getApplication()).mode.equals("add")) { barTitle = "Add to " + b.getString("warehouseone"); }
                if (((MyApplication)getApplication()).mode.equals("out")) { barTitle = "Transfer from "+ b.getString("warehouseone") + " to " + b.getString("warehousetwoi"); }
                if (((MyApplication)getApplication()).mode.equals("in")) {  barTitle = "Transfer to "+ b.getString("warehouseonei") + " from " + b.getString("warehousetwo");  }
                if (((MyApplication)getApplication()).mode.equals("stock")) {  barTitle = "Stock take at " + b.getString("warehouseone"); }
                setTitle(barTitle);

            }

        }

        ListView listView1 = (ListView)findViewById(R.id.list_view_items1);

        LayoutInflater inflater1 = getLayoutInflater();
        ViewGroup header1 = (ViewGroup)inflater1.inflate(R.layout.header_quotation, listView1, false);
        listView1.addHeaderView(header1, null, false);

        listView1.setAdapter(quotationsAdapter);

        ListView listView = (ListView)findViewById(R.id.list_view_items);

        listView.setAdapter(quotationsAdapter);

        if (b != null && b.getParcelable("item") != null)
        {
            LineItem li = (LineItem) b.getParcelable("item");

            quotationsAdapter.quotation.lineItems.add(li);
            quotationsAdapter.notifyDataSetChanged();
        }

        if (b != null && b.getParcelableArrayList("lineitems") != null)
        {
            quotationsAdapter.quotation.lineItems = b.getParcelableArrayList("lineitems");
            quotationsAdapter.notifyDataSetChanged();
        }

        quotationsAdapter.notifyDataSetChanged();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView parent, View view, int position, long id) {
                    itemSelectedPosition = position;
                    processItemSelected(quotationsAdapter.quotation.lineItems.get(position), "Update item", "", "QTY", position);
            }
        });

        TextView txtViewItems = findViewById(R.id.txtViewItems);
        txtViewItems.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                /*
                int min = 1;
                int max = 5;
                Random rn = new Random();
                int number = rn.nextInt(max - min + 1) + min;

                String itemcode = "1225063100035";
//              String itemcode = "2030";
*/
                myTTS.speak("Manual add", TextToSpeech.QUEUE_FLUSH, null);

                AlertDialog.Builder builder = new AlertDialog.Builder(QuotationAddActivity.this);
                builder.setTitle("Manual code entry");

                final TextView tx = new TextView(QuotationAddActivity.this);
                tx.setText("Item code");
                final EditText input = new EditText(QuotationAddActivity.this);
                input.setInputType(InputType.TYPE_CLASS_NUMBER);

                final TextView txd = new TextView(QuotationAddActivity.this);
                tx.setText("Item description");
                final EditText desc = new EditText(QuotationAddActivity.this);


                LinearLayout layout = new LinearLayout(QuotationAddActivity.this);
                layout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                layout.setOrientation(LinearLayout.VERTICAL);
                layout.setPadding(60, 0, 60, 0);

                layout.addView(tx);
                layout.addView(input);

                layout.addView(txd);
                layout.addView(desc);

                builder.setView(layout);

                builder.setPositiveButton("Add", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //add items to list
                        ScanInputMethod(input.getText().toString(), ((MyApplication)getApplication()).selectedWarehouse);
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
        });

        if (((MyApplication)getApplication()).mode.equals("receive"))
        {
            promptMode = false;
        }

        this.registerReceiver(this.mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

    //Main pending takes adaptor

        if (pc == null)
        {
            pc = new ProgressDialog(QuotationAddActivity.this);
            pc.setIndeterminate(false);
            pc.setCancelable(false);
        }

        if (b == null)
        {
            pc.setMessage("Loading stock list");
            pc.show();

            ((MyApplication) getApplication()).setResponse("");
            ((MyApplication) getApplication()).setRequest("88|" + ((MyApplication) getApplication()).stockTakeId + "|<EOF>");
            ((MyApplication) getApplication()).startCT();

            Timer t = new Timer();
            t.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    if (((MyApplication) getApplication()).getResponse().equals("error")) {
                        t.cancel();

                        runOnUiThread(new Runnable() {

                            @Override
                            public void run() {

                                try {

                                    pc.dismiss();

                                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(QuotationAddActivity.this);
                                    builder.setTitle("Please check connectivity and try again.");
                                    builder.setNeutralButton("Retry", new DialogInterface.OnClickListener() {
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
                    } else if (!((MyApplication) getApplication()).getResponse().equals("")) { //Search Item
                        t.cancel();

                        runOnUiThread(new Runnable() {

                            @Override
                            public void run() {

                                try {

                                    pc.dismiss();

                                    InputSource is = new InputSource();
                                    is.setCharacterStream(new StringReader(((MyApplication) getApplication()).getResponse()));

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

                                            Element itemCode = (Element) element2.getElementsByTagName("ItemCode").item(0);
                                            Element barcode = (Element) element2.getElementsByTagName("Barcode").item(0);
                                            Element itemName = (Element) element2.getElementsByTagName("ItemName").item(0);
                                            Element frgnName = (Element) element2.getElementsByTagName("FrgnName").item(0);
                                            Element onHand = (Element) element2.getElementsByTagName("OnHand").item(0);

                                            if (element2.getTextContent() != null) {
                                                LineItem li = new LineItem();
                                                li.code = itemCode.getTextContent().toString();
                                                li.barcode = barcode.getTextContent().toString();
                                                li.name = itemName.getTextContent().toString();
                                                li.description = frgnName.getTextContent().toString();
                                                li.quantity = Integer.parseInt(onHand.getTextContent().toString());
                                                ((MyApplication)getApplication()).takeAdaptor.quotation.lineItems.add(li);
                                            }
                                        }
                                    }

                                    int totalitems =  ((MyApplication)getApplication()).takeAdaptor.quotation.lineItems.size();
                                    myTTS.speak("Total " + totalitems + " items.", TextToSpeech.QUEUE_FLUSH, null);
                                    Toast.makeText(QuotationAddActivity.this, "Total: " + totalitems + " items.", Toast.LENGTH_LONG).show();

                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                }
            }, 500,1000);


        }



    }


    public void backgroundStartGroupTake(String whs)
    {
        String request = "14|" + whs + "|<EOF>" ;

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
                                if (((MyApplication)getApplication()).getResponse().contains("Success"))
                                {
                                    Toast.makeText(QuotationAddActivity.this, "Started group take", Toast.LENGTH_LONG).show();
                                }
                                else
                                {
                                    String err = ((MyApplication)getApplication()).getResponse();
                                    Toast.makeText(QuotationAddActivity.this, err, Toast.LENGTH_LONG).show();
                                }
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

    }

    @Override
    public void onInit(int initStatus) {
        if (initStatus == TextToSpeech.SUCCESS) {
            myTTS.setLanguage(Locale.US);
        }
        else if (initStatus == TextToSpeech.ERROR) {
            Toast.makeText(this, "Sorry! Text To Speech failed...", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onClick(View view) {

    }

    private void saveQuotation(String inputString, boolean closec, String username) throws IOException {


        ArrayList<LineItem> pending = determinePending();
        if (pending.size() > 0)
        {
            Toast.makeText(QuotationAddActivity.this, "Not all items have been updated. Please complete and then submit.", Toast.LENGTH_LONG).show();
            myTTS.speak("Please complete stock take for all items before saving.", TextToSpeech.QUEUE_FLUSH, null);
        }
        else
        {
            p.setMessage("Saving item data.");


            Long qDate = quotationsAdapter.quotation.datetime;
            String data = "";
            int i = 0;
            for (LineItem q: quotationsAdapter.quotation.lineItems)
            {
   //             if (q.submitted == 1) continue;
                if (!data.equals("")) data += ",";
                if (((MyApplication)getApplication()).mode.equals("stock"))
                {
                    data += q.id + "@" + q.code + "_" + q.selectedLot + "@" + q.qtychange + "@" + q.datetime + "@" + q.curQuantity + "V" + q.quantity + "@" + username + "@" + q.comments + "@" + q.image + "@" + q.bin;
                }
                else
                {
                    data += q.id + q.code +  "_" + q.selectedLot + "@" + q.curQuantity + "@" + username + "@"  + q.comments + "@" + q.image + "@" + q.bin;
                }

                i++;
            }

            ((MyApplication)getApplication()).totalsaved = i;

            SharedPreferences prf = getSharedPreferences("user_details",MODE_PRIVATE);
            String seriesName = prf.getString("seriesName" , null);
            int salesPerson = prf.getInt("salesPerson", -1);
            String inventorygl = prf.getString("inventorygl" , null);
            String whdata = prf.getString("whdata" , null);
            String loggedName = prf.getString("loggedName", "");

            String sentQ = "false";
            if (quotationsAdapter.quotation.sent)
            {
                sentQ = "true";
            }

            String whsentry = "";
            String related_to = ";related_to:";
            if (((MyApplication)getApplication()).mode.equals("stock"))
            {
                whsentry += "warehouse:" + ((MyApplication) getApplication()).selectedWarehouse + ";secondwarehouse:" + ((MyApplication) getApplication()).secondWarehouse + ";";
                whsentry += "supplier:" + ((MyApplication) getApplication()).selectedSupplier + ";";
                whsentry += "takerecord:" + quotationsAdapter.quotation.stockTakeId + ";";
            }

            if (((MyApplication)getApplication()).mode.equals("in"))
            {
                related_to = ";related_to:" + quotationsAdapter.quotation.related_to;
                whsentry = "warehouse:" + ((MyApplication) getApplication()).selectedWarehouse + ";secondwarehouse:" + quotationsAdapter.quotation.warehouseOneIntent + ";";
                whsentry += "warehousei:" + quotationsAdapter.quotation.warehouseOneIntent + ";secondwarehousei:" + quotationsAdapter.quotation.warehouseTwoIntent + ";";
            }
            if (((MyApplication)getApplication()).mode.equals("out"))
            {
                whsentry = "warehouse:" + ((MyApplication) getApplication()).selectedWarehouse + ";secondwarehouse:" + ((MyApplication) getApplication()).secondWarehouse + ";";
                whsentry += "warehousei:" + quotationsAdapter.quotation.warehouseOneIntent + ";secondwarehousei:" + quotationsAdapter.quotation.warehouseTwoIntent + ";";
            }

            whsentry += "category:" + ((MyApplication) getApplication()).categorychosen +";";

            String dataf = ";startDateTime:" + quotationsAdapter.quotation.datetime + ";sent:"+ sentQ +";seriesName:"+seriesName+";"+whsentry+"tabletUser:"+loggedName+";salesEmployee:"+String.valueOf(salesPerson)+";remarks:" + inputString + ";vehicle:" + quotationsAdapter.quotation.vehicletrack + ";inventorygl:" + inventorygl + ";whdata:"+ whdata + ";approvedby:;taketype:" + quotationsAdapter.quotation.stockTakeType;

            p.show();

            String request = "10|mode:" + ((MyApplication)getApplication()).mode + ";items:" + data + dataf + "|<EOF>" ;

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

                                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(QuotationAddActivity.this);
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
                                    p.dismiss();
                                    if (((MyApplication)getApplication()).getResponse().contains("Success"))
                                    {
                                        for (LineItem q: quotationsAdapter.quotation.lineItems) {
                                            q.submitted = 1;
                                        }

                                        String resp = ((MyApplication)getApplication()).getResponse();

                                        String[] itmdata = resp.split("\\|");
                                        for (String sitm: itmdata[1].split(";"))
                                        {
                                            String[] prts = sitm.split(":");
                                            String itemcode = prts[0];
                                            String itemname = prts[1];
                                            int qty = Integer.parseInt(prts[2]);
                                            for (LineItem q: quotationsAdapter.quotation.lineItems) {
                                                if (itemcode.equals(q.code))
                                                {
                                                    if (q.quantity == 0) q.quantity = qty;
                                                    if (q.name.equals(""))  q.name = itemname;
                                                }
                                            }

                                        }
                                        quotationsAdapter.notifyDataSetChanged();
                                        String err = ((MyApplication)getApplication()).getResponse() + "; " + ((MyApplication)getApplication()).totalsaved + " items";
                                        Toast.makeText(QuotationAddActivity.this, itmdata[0], Toast.LENGTH_LONG).show();

                                        quotationsAdapter.quotation.sent = true;

                                        try {
                                            ((MyApplication)getApplication()).removeSerializedQuotation(quotationsAdapter.quotation);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }

                                        finish();
                                    }
                                    else
                                    {
                                        String err = ((MyApplication)getApplication()).getResponse();
                                        Toast.makeText(QuotationAddActivity.this, err, Toast.LENGTH_LONG).show();
                                    }
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

        }


    }

    public void launchRemoveItemIntent()
    {
        quotationsAdapter.quotation.itemForRemoval =  quotationsAdapter.quotation.lineItems.get(itemSelectedPosition-1);
        AlertDialog.Builder builder = new AlertDialog.Builder(QuotationAddActivity.this);

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        quotationsAdapter.quotation.lineItems.remove(quotationsAdapter.quotation.itemForRemoval);
                        quotationsAdapter.notifyDataSetChanged();
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        break;
                }
            }
        };

        builder.setMessage("Remove item " + quotationsAdapter.quotation.lineItems.get(itemSelectedPosition-1).code).setPositiveButton("Yes", dialogClickListener).setNegativeButton("No", dialogClickListener).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_CAMERA_PERMISSION_CODE)
        {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                Toast.makeText(this, "camera permission granted", Toast.LENGTH_LONG).show();
                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, CAMERA_REQUEST);
            }
            else
            {
                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();
            }
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

        if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK)
        {
            Bitmap photo = (Bitmap) data.getExtras().get("data");
            imageView.setImageBitmap(photo);
        }

        if (requestCode == MY_DATA_CHECK_CODE) {
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                myTTS = new TextToSpeech(this, this);
            }
            else {
                Intent installTTSIntent = new Intent();
                installTTSIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                startActivity(installTTSIntent);
            }
        }
        if (requestCode == LOOKUP_ITEM_REQUEST && resultCode == RESULT_OK)
        {
            LineItem li = (LineItem) data.getParcelableExtra("item");
            Snackbar.make(findViewById(android.R.id.content).getRootView(), "Item code: " + li.code, Snackbar.LENGTH_LONG).setAction("Action", null).show();

            boolean exists = false;
            int cnt = 0;
            for (LineItem qli : quotationsAdapter.quotation.lineItems)
            {
                if (li.code.equals(qli.code) && li.warehouse.equals(qli.warehouse))
                {
                    exists = true;
                    quotationsAdapter.quotation.lineItems.get(cnt).quantity = li.quantity;
                }
                cnt++;
            }

            if (!exists)
            {
                quotationsAdapter.quotation.lineItems.add(li);
            }

            quotationsAdapter.notifyDataSetChanged();

        }
    }

    private static final String TAG = "IntentApiSample";
    private static final String ACTION_BARCODE_DATA = "com.honeywell.intent.action.SCAN_RESULT";
    private static final String ACTION_BARCODE_DATA1 = "com.honeywell.sample.action.MY_BARCODE_DATA";
    private static final String ACTION_BARCODE_DATA2 = "com.honeywell.sample.action.BARCODE_DATA";
    private static final String ACTION_CLAIM_SCANNER = "com.honeywell.aidc.action.ACTION_CLAIM_SCANNER";
    private static final String ACTION_RELEASE_SCANNER = "com.honeywell.aidc.action.ACTION_RELEASE_SCANNER";
    private static final String EXTRA_SCANNER = "com.honeywell.aidc.extra.EXTRA_SCANNER";
    private static final String EXTRA_PROFILE = "com.honeywell.aidc.extra.EXTRA_PROFILE";
    private static final String EXTRA_PROPERTIES = "com.honeywell.aidc.extra.EXTRA_PROPERTIES";

    private BroadcastReceiver barcodeDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e("BBC", intent.getAction());
            if (ACTION_BARCODE_DATA.equals(intent.getAction()) || ACTION_BARCODE_DATA1.equals(intent.getAction())) {
                /*
                These extras are available:
                "version" (int) = Data Intent Api version
                "aimId" (String) = The AIM Identifier
                API DOCUMENTATION
                Honeywell Android Data Collection Intent API
                "charset" (String) = The charset used to convert "dataBytes" to "data" string
                "codeId" (String) = The Honeywell Symbology Identifier
                "data" (String) = The barcode data as a string
                "dataBytes" (byte[]) = The barcode data as a byte array
                "timestamp" (String) = The barcode timestamp
                */
                Bundle bundle = intent.getExtras();
                if (bundle != null) {
                    for (String key : bundle.keySet()) {
                        Log.e("BBC", key + " : " + (bundle.get(key) != null ? bundle.get(key) : "NULL"));
                    }
                }

                String decoderesult = intent.getStringExtra("decode_rslt");

                if (bundle.containsKey("decode_rslt"))
                {
                    Log.e("BBC", bundle.getString("decode_rslt"));
                    ScanInputMethod(bundle.getString("decode_rslt").trim().replaceAll("[\\n\\t ]", ""), ((MyApplication)getApplication()).selectedWarehouse);
                }
                //if ()

                int version = intent.getIntExtra("version", 0);
                if (version >= 1) {
                    String aimId = intent.getStringExtra("aimId");
                    String charset = intent.getStringExtra("charset");
                    String codeId = intent.getStringExtra("codeId");
                    String data = intent.getStringExtra("data");
                    byte[] dataBytes = intent.getByteArrayExtra("dataBytes");
                    String dataBytesStr = bytesToHexString(dataBytes);
                    String timestamp = intent.getStringExtra("timestamp");
                    ScanInputMethod(data, ((MyApplication)getApplication()).selectedWarehouse);
                }
            }
        }
    };

    private Pattern pattern = Pattern.compile("-?\\d+(\\.\\d+)?");

    public boolean isNumeric(String strNum) {
        if (strNum == null) {
            return false;
        }
        return pattern.matcher(strNum).matches();
    }

    public void ScanInputMethod(String searchTerm, String warehouse) {

        ((MyApplication) getApplication()).searchTerm = searchTerm;

        try {
            ((MyApplication) getApplication()).storeSerializedQuotation(quotationsAdapter.quotation);
        } catch (IOException e) {
            e.printStackTrace();
        }

            try
            {
                ad.dismiss();
            }
            catch (Exception ade)
            {

            }


                String searchWhs = ((MyApplication) getApplication()).selectedWarehouse;
                String searchSupplier = ((MyApplication) getApplication()).selectedSupplier;

                LineItem li = new LineItem();
                for (LineItem lit : ((MyApplication) getApplication()).takeAdaptor.quotation.lineItems)
                {
                    Log.d("Check", lit.code + "; " + lit.barcode);
                    if (lit.code.equals(searchTerm) || Arrays.asList(lit.barcode.split(",")).contains(searchTerm))
                    {
                        li.code = lit.code.toString();
                        li.name = lit.name.toString();
                        li.barcode = lit.barcode.toString();
                        li.description = lit.description.toString();
                        li.quantity = lit.quantity;
                        break;
                    }
                }

                ///

        if (li.code != null && !li.code.equals("")) {
            queryCount(li, "");

        } else {

            Toast.makeText(QuotationAddActivity.this, "Code '"+searchTerm+"' does not exist.", Toast.LENGTH_SHORT).show();
            myTTS.speak("Code does not exist. Would you like to add?", TextToSpeech.QUEUE_FLUSH, null);

            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(QuotationAddActivity.this);
            builder.setTitle("[0] Item " + searchTerm + " not in stock list.");

            LinearLayout layout2 = new LinearLayout(QuotationAddActivity.this);
            layout2.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            layout2.setOrientation(LinearLayout.VERTICAL);
            layout2.setPadding(50, 30, 50, 0);

            final TextView tv1 = new TextView(QuotationAddActivity.this);
            tv1.setText("Would you like to add it anyway?");

            final TextView tx = new TextView(QuotationAddActivity.this);
            tx.setText("Item Description: ");
            final EditText desc = new EditText(QuotationAddActivity.this);

            layout2.addView(tv1);

            layout2.addView(tx);
            layout2.addView(desc);

            builder.setView(layout2);

            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    try {

                        li.code = searchTerm;
                        li.name = "ADDED";
                        li.description = desc.getText().toString();
                        li.quantity = 0;
                        queryCount(li, "");

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            builder.setNegativeButton("No", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });

            builder.show();



        }




                /*
                Timer t = new Timer();
                t.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        if (((MyApplication) getApplication()).getResponse().equals("error")) {
                            t.cancel();

                            runOnUiThread(new Runnable() {

                                @Override
                                public void run() {

                                    try {

                                        pc.dismiss();

                                        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(QuotationAddActivity.this);
                                        builder.setTitle("Please check connectivity and try again.");
                                        builder.setNeutralButton("Retry", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                ScanInputMethod(((MyApplication) getApplication()).searchTerm, ((MyApplication)getApplication()).selectedWarehouse);
                                            }
                                        });

                                        builder.show();
                                    } catch (Exception ep) {
                                        ep.printStackTrace();
                                    }
                                }
                            });
                        } else if (!((MyApplication) getApplication()).getResponse().equals("")) { //Search Item
                            t.cancel();

                            runOnUiThread(new Runnable() {

                                @Override
                                public void run() {

                                    try {

                                        pc.dismiss();
                                        processScan(((MyApplication) getApplication()).getResponse(), searchTerm);


                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                        }
                    }
                }, 500,1000);
                */
    }

    public void processScan(String resp, String searchTerm) throws ParserConfigurationException, IOException, SAXException {
/*
        InputSource is = new InputSource();
        is.setCharacterStream(new StringReader(resp));
        LineItem li = new LineItem();

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

                Element itemCode = (Element) element2.getElementsByTagName("ItemCode").item(0);
                Element itemName = (Element) element2.getElementsByTagName("ItemName").item(0);
                Element frgnName = (Element) element2.getElementsByTagName("FrgnName").item(0);
                Element onHand = (Element) element2.getElementsByTagName("OnHand").item(0);

                if (element2.getTextContent() != null) {
                    li.code = itemCode.getTextContent().toString();
                    li.name = itemName.getTextContent().toString();
                    li.description = frgnName.getTextContent().toString();
                    li.quantity = Integer.parseInt(onHand.getTextContent().toString());
                }
            }
            break;
        }
*/

        LineItem li = new LineItem();
        for (LineItem lit : ((MyApplication) getApplication()).takeAdaptor.quotation.lineItems)
        {
            if (lit.code.equals(searchTerm) || lit.id.equals(searchTerm))
            {
                li.code = lit.code.toString();
                li.name = lit.name.toString();
                li.description = lit.description.toString();
                li.quantity = lit.quantity;
                break;
            }
        }

        if (li.code != null && !li.code.equals("")) {
            queryCount(li, "");

        } else {

            Toast.makeText(QuotationAddActivity.this, "Code '"+searchTerm+"' does not exist.", Toast.LENGTH_SHORT).show();
            myTTS.speak("Code does not exist. Would you like to add?", TextToSpeech.QUEUE_FLUSH, null);


            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(QuotationAddActivity.this);
            builder.setTitle("[1] Item " + searchTerm + " not in stock list.");

            LinearLayout layout2 = new LinearLayout(QuotationAddActivity.this);
            layout2.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            layout2.setOrientation(LinearLayout.VERTICAL);
            layout2.setPadding(50, 30, 50, 0);

            final TextView tv1 = new TextView(QuotationAddActivity.this);
            tv1.setText("Would you like to add it anyway?");

            layout2.addView(tv1);
            builder.setView(layout2);

            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    try {

                        li.code = searchTerm;
                        li.name = "ADDED";
                        li.description = "";
                        li.quantity = 0;
                        queryCount(li, "");

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            builder.setNegativeButton("No", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });

            builder.show();



        }

    }


    public void queryCount(LineItem li, String matchedlot)
    {

        boolean found = false;
        for (LineItem l : quotationsAdapter.quotation.lineItems)
        {
            if (l.code.equals(li.code))
            {
                l.curQuantity = l.curQuantity + 1;
                found = true;
                myTTS.speak(l.curQuantity + "", TextToSpeech.QUEUE_FLUSH, null);
                break;
            }
        }

        if (!found)
        {
            li.curQuantity = 1;
            quotationsAdapter.quotation.lineItems.add(li);
            myTTS.speak("1", TextToSpeech.QUEUE_FLUSH, null);
        }

        quotationsAdapter.notifyDataSetChanged();


/*
        try {
            saveQuotation("", false);
        } catch (IOException e) {
            e.printStackTrace();
        }

 */
        //builder

    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(barcodeDataReceiver);
        releaseScanner();
    }
    private void claimScanner() {
        Bundle properties = new Bundle();
        properties.putBoolean("DPR_DATA_INTENT", true);
        properties.putString("DPR_DATA_INTENT_ACTION", ACTION_BARCODE_DATA);
        sendBroadcast(new Intent(ACTION_CLAIM_SCANNER)
                .putExtra(EXTRA_SCANNER, "dcs.scanner.imager")
                .putExtra(EXTRA_PROFILE, "CFP")
                .putExtra(EXTRA_PROPERTIES, properties)
        );
    }
    private void releaseScanner() {
        sendBroadcast(new Intent(ACTION_RELEASE_SCANNER));
    }

    private String bytesToHexString(byte[] arr) {
        String s = "[]";
        if (arr != null) {
            s = "[";
            for (int i = 0; i < arr.length; i++) {
                s += "0x" + Integer.toHexString(arr[i]) + ", ";
            }
            s = s.substring(0, s.length() - 2) + "]";
        }
        return s;
    }
}
