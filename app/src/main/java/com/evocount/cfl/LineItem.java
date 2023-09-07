package com.evocount.cfl;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.UUID;

public class LineItem implements Parcelable, Serializable {

    public String id;
    public String name;
    public String barcode;
    public String code;
    public String description;
    public int quantity;
    public int curQuantity;
    public String warehouse;
    public Long datetime;
    public int qtychange;
    public String[] lots;
    public String selectedLot;
    public String bin;
    public String lot;
    public String image;
    public String pallet;
    public String comments;
    public int submitted;

    public LineItem()
    {
        UUID uuid = UUID.randomUUID();
        id = uuid.toString();
        quantity = 0;
        curQuantity = 0;
        name = "";
        code = "";
        barcode = "";
        description = "";
        warehouse = "";
        datetime = System.currentTimeMillis();
        selectedLot = "";
        lot = "";
        qtychange = 0;
        image= "";
        pallet = "";
        comments = "";
        submitted = 0;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(id);
        out.writeInt(quantity);
        out.writeInt(curQuantity);
        out.writeString(name);
        out.writeString(code);
        out.writeString(barcode);
        out.writeString(description);
        out.writeString(warehouse);
        out.writeLong(datetime);
        out.writeString(selectedLot);
        out.writeString(lot);
        out.writeInt(qtychange);
        out.writeString(image);
        out.writeString(pallet);
        out.writeString(comments);
        out.writeInt(submitted);
    }

    public static final Parcelable.Creator<LineItem> CREATOR = new Parcelable.Creator<LineItem>() {
        public LineItem createFromParcel(Parcel in) {
            return new LineItem(in);
        }

        public LineItem[] newArray(int size) {
            return new LineItem[size];
        }
    };

    private LineItem(Parcel in) {
        id = in.readString();
        quantity = in.readInt();
        curQuantity = in.readInt();
        name = in.readString();
        code = in.readString();
        barcode = in.readString();
        description = in.readString();
        warehouse = in.readString(); //to get
        datetime = in.readLong();
        selectedLot = in.readString();
        lot = in.readString();
        qtychange = in.readInt();
        image = in.readString();
        pallet = in.readString();
        comments = in.readString();
        submitted = in.readInt();
    }
}
