package com.evocount.cfl;

import java.io.Serializable;
import java.util.ArrayList;

public class Quotation implements Serializable {

    private static final long serialVersionUID = -7060210544600464481L;
    public String id;
    public Long datetime;
    public LineItem itemForRemoval;
    public String remarks;
    public String status;
    public String numAtCard;
    public int vatpercent;
    public String warehouseOne;
    public String warehouseTwo;
    public String warehouseOneIntent;
    public String warehouseTwoIntent;
    public String mode;
    public boolean sent;
    public String category;
    public String supplier;
    public String vehicletrack;
    public int related_to;
    public String stockTakeType;
    public int stockTakeId;

    public ArrayList<LineItem> lineItems = new ArrayList<LineItem>();

    public Quotation(int vatperc)
    {
        this.lineItems = new ArrayList<LineItem>();
        this.datetime = System.currentTimeMillis();
        this.remarks = "";
        this.lineItems = new ArrayList<LineItem>();
        this.status = "";
        this.numAtCard = "";
        this.vatpercent = vatperc;
        this.warehouseOne = "";
        this.warehouseTwo = "";
        this.warehouseOneIntent = "";
        this.warehouseTwoIntent = "";
        this.mode = "stock";
        this.sent = false;
        this.category = "";
        this.supplier = "";
        this.vehicletrack = "";
        this.related_to = 0;
        this.stockTakeType = "";
        this.stockTakeId = 0;
    }
}
