package com.evocount.cfl;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.AsyncTask;

import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.evolabs.haco.count.fpstock.R;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class LoginActivity extends AppCompatActivity {

    private UserLoginTask mAuthTask = null;

    SharedPreferences pref;

    // UI references.
    private AutoCompleteTextView mUsernameView;
    private EditText mPasswordView;
    public String u_loggedName;
    public int u_vatpercent;
    public String u_series;
    public String u_userId;
    public String u_warehouse;
    public String u_inventory_gl;
    public String u_warehouse_data;
    public int u_salesPerson;
    public int u_additem = 0;
    public int u_stocktake = 0;
    public int u_stocktransfer = 0;
    public int u_discount;
    private View mProgressView;
    private View mLoginFormView;
    private String u_quotations;
    private String lastError = "Incorrect credentials or network unavailable.";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        pref = getSharedPreferences("user_details",MODE_PRIVATE);

        if (!pref.getString("loggedName", "").equals(""))
        {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        }

        mUsernameView = (AutoCompleteTextView) findViewById(R.id.username);

        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mSignInButton = (Button) findViewById(R.id.sign_in_button);
        mSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);

        ImageView imageView =(ImageView) findViewById(R.id.imageView) ;
        imageView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                setIP();
            }
        });


    }

    public void setIP()
    {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(LoginActivity.this);
        builder.setTitle("Server IP Setup");

        LinearLayout layout2 = new LinearLayout(LoginActivity.this);
        layout2.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        layout2.setOrientation(LinearLayout.VERTICAL);
        layout2.setPadding(50, 30, 50, 0);

        final TextView tv12 = new TextView(LoginActivity.this);
        tv12.setText("Enter Socket IP");

        final EditText input2 = new EditText(LoginActivity.this);
        input2.setText(((MyApplication)getApplication()).getSockIP());

        layout2.addView(tv12);
        layout2.addView(input2);

        builder.setView(layout2);

        builder.setPositiveButton("Update", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    ((MyApplication)getApplication()).saveSockIP(input2.getText().toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        builder.show();

    }

    private void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mUsernameView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String username = mUsernameView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;


        // Check for a valid username.
        if (TextUtils.isEmpty(username)) {
            mUsernameView.setError(getString(R.string.error_field_required));
            focusView = mUsernameView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mAuthTask = new UserLoginTask(username, password);
            mAuthTask.execute((Void) null);
        }
    }

    private boolean isPasswordValid(String password) {
        return password.length() > 4;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String mUsername;
        private final String mPassword;

        UserLoginTask(String username, String password) {
            mUsername = username;
            mPassword = password;
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
        protected Boolean doInBackground(Void... params) {

                Log.d("LOGIN ACTION", "CC");
                ((MyApplication) getApplication()).loggedin = 0;

                ((MyApplication)getApplication()).setResponse("");
                ((MyApplication)getApplication()).setRequest("995|" + mUsername + ":" + MD5(mPassword) + "|<EOF>");
                ((MyApplication)getApplication()).startCT();

            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            showProgress(false);

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


                                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(LoginActivity.this);
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
                                    if (((MyApplication)getApplication()).getResponse().contains("OKAPI"))
                                    {
                                        String[] rs = ((MyApplication)getApplication()).getResponse().split(";");
                                        for (String r : rs)
                                        {
                                            Log.d("ITEM", r);
                                            String[] s = r.split(":");

                                            if (s[0].equals("loggedName")) u_loggedName = s[1].trim();
                                            if (s[0].equals("userId")) u_userId = s[1].trim();
                                            ((MyApplication) getApplication()).loggedin = 1;
                                            Log.d("LOGGED" , "TRUE");


                                                SharedPreferences.Editor editor = pref.edit();
                                                editor.putString("userId", u_userId);
                                                editor.putString( "loggedName", u_loggedName);
                                                editor.commit();

                                                //get saved quotations is newer
                                                if (u_quotations != null && !u_quotations.equals("NULL") && !u_quotations.equals("N"))
                                                {
                                                    HashMap<String, Quotation> quotations = new HashMap<String, Quotation>();

                                                    for (String qs : u_quotations.split(","))
                                                    {
                                                        try {
                                                            Log.d("DB Quotation" , qs);
                                                            Quotation q = (Quotation) fromString(qs);
                                                            quotations.put(q.datetime.toString(), q);
                                                        } catch (IOException e) {
                                                            e.printStackTrace();
                                                        } catch (ClassNotFoundException e) {
                                                            e.printStackTrace();
                                                        }
                                                    }

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
                                                }
                                                else
                                                {
                                                    HashMap<String, Quotation> quotations = new HashMap<String, Quotation>();

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
                                                }

                                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                                startActivity(intent);
                                                finish();

                                        }

                                    }
                                    else
                                    {
                                        String err = ((MyApplication)getApplication()).getResponse();
                                        Toast.makeText(LoginActivity.this, err, Toast.LENGTH_LONG).show();

                                        mPasswordView.setError(lastError);
                                        mPasswordView.requestFocus();

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
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }

        private Object fromString(String s) throws IOException, ClassNotFoundException {
            byte[] data = android.util.Base64.decode(s, Base64.URL_SAFE);
            ObjectInputStream ois = new ObjectInputStream(
                    new ByteArrayInputStream(data));
            Object o = ois.readObject();
            ois.close();
            return o;
        }

    }
}

