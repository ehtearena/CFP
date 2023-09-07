package com.evocount.cfl;

import java.io.Serializable;

public class QualityAnalysis implements Serializable {

    private static final long serialVersionUID = -7060210544600464481L;
    public String total;
    public String qa;
    public String pcu;
    public String lot;
    public String allpallets;
    public String pt;

    public QualityAnalysis()
    {
        this.total = "";
        this.qa = "";
        this.pt = "";
        this.lot = "";
        this.pcu = "";
        this.allpallets = "";
    }
}
