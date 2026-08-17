package com.ybm.banksimulator.service;

import com.ybm.banksimulator.model.PaymentRequest;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
public class EpiTransactionBuilder {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public String buildRawTransaction(PaymentRequest request) {

        return "fldClientCode=" + value(request.getFldClientCode()) +
                "&fldMerchCode=" + value(request.getFldMerchCode()) +
                "&fldTxnCurr=" + value(request.getFldTxnCurr()) +
                "&fldTxnAmt=" + value(request.getFldTxnAmt()) +
                "&fldTxnScAmt=" + value(request.getFldTxnScAmt()) +
                "&fldMerchRefNbr=" + value(request.getFldMerchRefNbr()) +
                "&fldDatTimeTxn=" + formatDateTime(request) +
                "&fldRef1=" + value(request.getFldRef1()) +
                "&fldRef2=" + value(request.getFldRef2()) +
                "&fldRef3=" + value(request.getFldRef3()) +
                "&fldRef4=" + value(request.getFldRef4()) +
                "&fldRef5=" + value(request.getFldRef5()) +
                "&fldRef6=" + value(request.getFldRef6()) +
                "&fldRef7=" + value(request.getFldRef7()) +
                "&fldRef8=" + value(request.getFldRef8()) +
                "&fldRef9=" + value(request.getFldRef9()) +
                "&fldRef10=" + value(request.getFldRef10()) +
                "&fldRef11=" + value(request.getFldRef11()) +
                "&fldDate1=" + value(request.getFldDate1()) +
                "&fldDate2=" + value(request.getFldDate2()) +
                "&RU=" + value(request.getRu()) +
                "&fldClientAcctNo=" + value(request.getFldClientAcctNo())+
                "&bankid=" + value(request.getBankid());
    }

    private String value(Object value) {
        return value == null ? "" : value.toString();
    }

    private String formatDateTime(PaymentRequest request) {
        if (request.getFldDatTimeTxn() == null) {
            return "";
        }

        return request.getFldDatTimeTxn()
                .format(DATE_TIME_FORMATTER);
    }
}