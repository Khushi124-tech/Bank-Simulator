

package com.ybm.banksimulator.model;

public class BankTransactionState {


    private String pid;
    private String encdata;

    private PaymentRequest paymentRequest;

    private boolean pidValid;
    private boolean checksumValid;

    private String status;

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

    public PaymentRequest getPaymentRequest() {
        return paymentRequest;
    }

    public void setPaymentRequest(PaymentRequest paymentRequest) {
        this.paymentRequest = paymentRequest;
    }

    public boolean isPidValid() {
        return pidValid;
    }

    public void setPidValid(boolean pidValid) {
        this.pidValid = pidValid;
    }

    public boolean isChecksumValid() {
        return checksumValid;
    }

    public void setChecksumValid(boolean checksumValid) {
        this.checksumValid = checksumValid;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

}
