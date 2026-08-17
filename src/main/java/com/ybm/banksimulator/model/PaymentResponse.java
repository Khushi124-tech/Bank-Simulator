package com.ybm.banksimulator.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentResponse {

    private String fldClientCode;
    private String fldMerchCode;
    private String fldTxnCurr;

    private BigDecimal fldTxnAmt;
    private BigDecimal fldTxnScAmt;

    private String fldMerchRefNbr;

    private String flgSuccess;

    private String fldVerify;

    private LocalDateTime fldDatTimeTxn;

    private String bankRefNo;

    private String fldRef2;

    private String message;

    /*
     * Encrypted representation of this response (raw EPI string
     * + checksum, AES encrypted) - the same shape the bank would
     * POST to the gateway's S2S callback endpoint
     * (/payment/result) in a real integration.
     */
    private String encdata;

    public String getFldClientCode() {
        return fldClientCode;
    }

    public void setFldClientCode(String fldClientCode) {
        this.fldClientCode = fldClientCode;
    }

    public String getFldMerchCode() {
        return fldMerchCode;
    }

    public void setFldMerchCode(String fldMerchCode) {
        this.fldMerchCode = fldMerchCode;
    }

    public String getFldTxnCurr() {
        return fldTxnCurr;
    }

    public void setFldTxnCurr(String fldTxnCurr) {
        this.fldTxnCurr = fldTxnCurr;
    }

    public BigDecimal getFldTxnAmt() {
        return fldTxnAmt;
    }

    public void setFldTxnAmt(BigDecimal fldTxnAmt) {
        this.fldTxnAmt = fldTxnAmt;
    }

    public BigDecimal getFldTxnScAmt() {
        return fldTxnScAmt;
    }

    public void setFldTxnScAmt(BigDecimal fldTxnScAmt) {
        this.fldTxnScAmt = fldTxnScAmt;
    }

    public String getFldMerchRefNbr() {
        return fldMerchRefNbr;
    }

    public void setFldMerchRefNbr(String fldMerchRefNbr) {
        this.fldMerchRefNbr = fldMerchRefNbr;
    }

    public String getFlgSuccess() {
        return flgSuccess;
    }

    public void setFlgSuccess(String flgSuccess) {
        this.flgSuccess = flgSuccess;
    }

    public String getFldVerify() {
        return fldVerify;
    }

    public void setFldVerify(String fldVerify) {
        this.fldVerify = fldVerify;
    }

    public LocalDateTime getFldDatTimeTxn() {
        return fldDatTimeTxn;
    }

    public void setFldDatTimeTxn(LocalDateTime fldDatTimeTxn) {
        this.fldDatTimeTxn = fldDatTimeTxn;
    }

    public String getBankRefNo() {
        return bankRefNo;
    }

    public void setBankRefNo(String bankRefNo) {
        this.bankRefNo = bankRefNo;
    }

    public String getFldRef2() {
        return fldRef2;
    }

    public void setFldRef2(String fldRef2) {
        this.fldRef2 = fldRef2;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getEncdata() {
        return encdata;
    }

    public void setEncdata(String encdata) {
        this.encdata = encdata;
    }
}