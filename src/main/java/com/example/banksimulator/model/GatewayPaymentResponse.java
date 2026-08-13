package com.example.banksimulator.model;

public class GatewayPaymentResponse {

    private String bankUrl;

    private String pid;

    private String status;

    private PaymentRequest request;


    public GatewayPaymentResponse() {
    }


    public GatewayPaymentResponse(
            String bankUrl,
            String pid,
            String status,
            PaymentRequest request) {

        this.bankUrl = bankUrl;
        this.pid = pid;
        this.status = status;
        this.request = request;
    }


    public String getBankUrl() {
        return bankUrl;
    }


    public void setBankUrl(String bankUrl) {
        this.bankUrl = bankUrl;
    }


    public String getPid() {
        return pid;
    }


    public void setPid(String pid) {
        this.pid = pid;
    }


    public String getStatus() {
        return status;
    }


    public void setStatus(String status) {
        this.status = status;
    }


    public PaymentRequest getRequest() {
        return request;
    }


    public void setRequest(
            PaymentRequest request) {

        this.request = request;
    }
}