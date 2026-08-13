package com.example.banksimulator.model;

public class BankPaymentRequest {

    private String pid;
    private String encdata;

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public String getEncdata() {
        return encdata;
    }

    public void setEncdata(String encdata) {
        this.encdata = encdata;
    }
}