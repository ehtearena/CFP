package com.evocount.cfl;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MyApplication extends Application {

    private static final int SERVERPORT = 15001;

    List<String> partName;
    List<String> partId;
    List<KeyPairBoolData> partNameCat;

    public QuotationsAdapter takeAdaptor = new QuotationsAdapter(MyApplication.this, 0);
    public ArrayList<LineItem> takeLi = new ArrayList<LineItem>();
    public int selectedLotRemaining = 0;
    public String selectedSupplier = "";
    public String searchTerm = "";
    public int loggedin = 0;
    private String request = "";
    private String response = "";
    public String mode = "";
    public String selectedWarehouse = "";
    public String secondWarehouse = "";
    public boolean secondWarehouseEnabled = false;
    public String categorychosen = "";
    public boolean offlineMode = false;
    public String currentIP = "";
    public int totalsaved = 0;
    public int stockTakeId = 0;

    HashMap<String, ArrayList<LineItem>> warehouseitems = new HashMap<String, ArrayList<LineItem>>();



    private static MyApplication instance;

    public MyApplication() {
        instance = this;
    }

    public static Context getContext() {
        if (instance == null) {
            instance = new MyApplication();
        }
        return instance;
    }

    public void startCT() {
        new Thread(new ClientThread()).start();
    }

    public void setRequest(String request) {
        this.request = request;
    }

    public void setResponse(String response) {
        this.response = response;
    }


    public void saveSockIP(String ip) throws IOException {

        FileOutputStream fileOut = null;
        try {
            fileOut = getApplicationContext().openFileOutput("ipsock.ser", Context.MODE_PRIVATE);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(ip);
            out.close();
            fileOut.close();

        } catch (IOException i) {
            i.printStackTrace();
        }
    }

    public String getSockIP() {

        try {
            FileInputStream fis = getApplicationContext().openFileInput("ipsock.ser");
            ObjectInputStream ois = new ObjectInputStream(fis);
            currentIP = (String) ois.readObject();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return currentIP;
    }

    public String getResponse() {
        if (this.response == null)
        {
            return "";
        }

        return this.response;
    }



    class ClientThread implements Runnable {

        @Override
        public void run() {

            try {

                response = "";
                if (isOnline())
                {
                    Log.d("XERT-REQ", request);

                    if (response.equals(""))
                    {
                        InetAddress serverAddr = InetAddress.getByName(getSockIP());

                        Socket socket = new Socket(serverAddr, SERVERPORT);
                        socket.setSoTimeout(10*1500);
                        PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);

                        out.println(request);

                        BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                        response = input.readLine();

                        Log.d("XERT-IN", "<>");

                    }

                    Log.d("XERT-RES", response);


                }
                else
                {
                    response = "error";
                }

            } catch (SocketTimeoutException e) {
                response = "error";
            } catch (Exception e) {
                response = "error";
            }
        }
    }

    public boolean isOnline() {

        Log.d("XERT","IP: " + getSockIP() + "; Port:" + SERVERPORT);
        boolean b = true;
        try{
            InetSocketAddress sa = new InetSocketAddress(getSockIP(), SERVERPORT);
            Socket ss = new Socket();
            ss.connect(sa, 3000);
            ss.close();
        }catch(Exception e) {
            b = false;
        }
        return b;
    }
    public void storeSerializedQuotation(Quotation quotation) throws IOException
    {
        HashMap<String, Quotation> quotations = this.retrieveSerializedQuotations();
        quotations.put(quotation.datetime.toString(), quotation);
        saveSerializedQuotations(quotations);
    }

    public void removeSerializedQuotationByDate(Long qdate) throws IOException {
        HashMap<String, Quotation> quotations = this.retrieveSerializedQuotations();
        quotations.remove(qdate.toString());
        saveSerializedQuotations(quotations);
    }

    public void removeSerializedQuotation(Quotation quotation) throws IOException
    {
        HashMap<String, Quotation> quotations = this.retrieveSerializedQuotations();
        quotations.remove(quotation.datetime.toString());
        saveSerializedQuotations(quotations);
    }

    public HashMap<String, Quotation> retrieveSerializedQuotations() {
        HashMap<String, Quotation> quotations = new HashMap<String, Quotation>();

        try {
            FileInputStream fis = getApplicationContext().openFileInput("quotations.ser");
            ObjectInputStream ois = new ObjectInputStream(fis);
            quotations = (HashMap<String, Quotation>) ois.readObject();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return quotations;
    }

    public void saveSerializedQuotations(HashMap<String, Quotation> quotations) throws IOException {

        FileOutputStream fileOut = null;
        try {
            fileOut = getApplicationContext().openFileOutput("quotations.ser", Context.MODE_PRIVATE);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(quotations);
            out.close();
            fileOut.close();

        } catch (IOException i) {
            i.printStackTrace();
        }

        String[] serializedQuotations = new String[quotations.size()];
        int i = 0;
        for (String key : quotations.keySet())
        {
            Quotation q = quotations.get(key);
            serializedQuotations[i] = toString(q);
            i++;
        }

        //Send strings to DB for user.
        SharedPreferences prf = getSharedPreferences("user_details", MODE_PRIVATE);
        String userId = prf.getString("userId", null);

        UserSerializedQuotationsSaveTask saveSerQTask = new UserSerializedQuotationsSaveTask(serializedQuotations, userId);
        saveSerQTask.execute((Void) null);
    }

    private String toString(Serializable o) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(o);
        oos.close();
        return android.util.Base64.encodeToString(baos.toByteArray(), Base64.URL_SAFE);
    }

    public class UserSerializedQuotationsSaveTask extends AsyncTask<Void, Void, Boolean> {

        String userId;
        String[] quotations;
        UserSerializedQuotationsSaveTask(String[] qtns, String usrId ) {
            this.quotations = qtns;
            this.userId = usrId;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
/* REMOVED FOR DEMO
            try {
                Log.d("USER SER TASK", "SENDING");

//                URL url = new URL("http://fpl.echo-systems.co.ke/user/userquotationssave")
//
                URL url = new URL("http://192.168.0.7/user/userquotationssave");
//                URL url = new URL("http://192.168.100.9/user/userquotationssave");
//                URL url = new URL("http://10.0.2.2/user/userquotationssave");
                String data = "userId=" + userId;
                int q = 0;
                for (String s: quotations)
                {
                    data += "&";
                    data += "quotatsaion" + q + "=" + s;
                    q++;
                }

                URLConnection conn = url.openConnection();
                conn.setDoOutput(true);

                OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
                wr.write( data );
                wr.flush();

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line = null;

                while((line = reader.readLine()) != null)
                {
                    sb.append(line + "\n");
                }

                String response = sb.toString();
                Log.d("USER SER TASK", response);

                if (response.contains("OKAPI"))
                {
                    return true;
                }
                else
                {
                    return false;
                }

            } catch (Exception e) {
                return false;
            }

 */
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            if (success) {
            } else {
            }
        }

        @Override
        protected void onCancelled() {
        }
    }
}

