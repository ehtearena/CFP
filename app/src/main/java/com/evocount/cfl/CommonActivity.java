package com.evocount.cfl;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

public abstract class CommonActivity extends AppCompatActivity {


    boolean scannerPopupisActive = false;
    QuotationsAdapter quotationsAdapter;
    LineItemAdapter lineItemAdapter;
    long delay = 1500; // 1 seconds after user stops typing
    long last_text_edit = 0;
    EditText inputText;
    int runnable_triggered = 0;
    ProgressDialog pc;
    int action = 0;
    AlertDialog ad;

    public void updateSupplier()
    {
    }

    public void updateCategory()
    {
    }

    protected void itemAddProcess(int action) {
        this.action = action;

        pc = new ProgressDialog(CommonActivity.this);
        pc.setIndeterminate(false);
        pc.setCancelable(false);

        runnable_triggered = 0;
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);

        builder.setTitle("Scan item");

        final EditText input = new EditText(this);

        builder.setView(input);

/*
        builder.setPositiveButton("Proceed", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                ScanInputMethod(input.getText().toString());
            }
        });
 */

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        ad = builder.create();

        ad.show();

        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                inputText = input;
                if (s.length() > 0) {
                    last_text_edit = System.currentTimeMillis();
                   // handler.postDelayed(input_finish_checker, delay);
                } else {

                }
            }
        });

    }
/*

    Handler handler = new Handler();
    private Runnable input_finish_checker = new Runnable() {
        public void run() {
            if (System.currentTimeMillis() > (last_text_edit + delay - 500)) {
                if (runnable_triggered == 0) {
                    runnable_triggered = 1;
                    ad.dismiss();
                    ScanInputMethod(inputText.getText().toString(), ((MyApplication)getApplication()).selectedWarehouse);
                }
            }
        }
    };


/*
 */



}
