package com.ybm.banksimulator.service;

import com.ybm.banksimulator.model.PaymentResponse;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
public class EpiResponseBuilder {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public String buildRawResponse(PaymentResponse response) {

        if (response == null) {
            throw new IllegalArgumentException(
                    "Payment response cannot be null"
            );
        }

        return "fldClientCode=" + value(response.getFldClientCode()) +
                "&fldMerchCode=" + value(response.getFldMerchCode()) +
                "&fldTxnCurr=" + value(response.getFldTxnCurr()) +
                "&fldTxnAmt=" + value(response.getFldTxnAmt()) +
                "&fldTxnScAmt=" + value(response.getFldTxnScAmt()) +
                "&fldMerchRefNbr=" + value(response.getFldMerchRefNbr()) +
                "&flgSuccess=" + value(response.getFlgSuccess()) +
                "&fldDatTimeTxn=" + formatDateTime(response) +
                "&BankRefNo=" + value(response.getBankRefNo()) +
                "&fldRef2=" + value(response.getFldRef2()) +
                "&Message=" + value(response.getMessage()) +
                "&fldVerify=" + value(response.getFldVerify());
    }

    private String value(Object value) {
        return value == null ? "" : value.toString();
    }

    private String formatDateTime(PaymentResponse response) {

        if (response.getFldDatTimeTxn() == null) {
            return "";
        }

        return response.getFldDatTimeTxn()
                .format(DATE_TIME_FORMATTER);
    }
}