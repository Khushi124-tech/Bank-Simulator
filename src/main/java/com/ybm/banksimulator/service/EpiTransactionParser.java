package com.ybm.banksimulator.service;

import com.ybm.banksimulator.model.PaymentRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class EpiTransactionParser {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public PaymentRequest parse(String rawTransaction) {

        if (rawTransaction == null || rawTransaction.isBlank()) {
            throw new IllegalArgumentException(
                    "Raw transaction cannot be empty"
            );
        }

        Map<String, String> fields = parseFields(rawTransaction);

        PaymentRequest request = new PaymentRequest();

        request.setFldClientCode(fields.get("fldClientCode"));
        request.setFldMerchCode(fields.get("fldMerchCode"));
        request.setFldTxnCurr(fields.get("fldTxnCurr"));

        request.setFldTxnAmt(
                new BigDecimal(fields.get("fldTxnAmt"))
        );

        request.setFldTxnScAmt(
                new BigDecimal(fields.get("fldTxnScAmt"))
        );

        request.setFldMerchRefNbr(
                fields.get("fldMerchRefNbr")
        );

        request.setFldDatTimeTxn(
                LocalDateTime.parse(
                        fields.get("fldDatTimeTxn"),
                        DATE_TIME_FORMATTER
                )
        );

        request.setFldRef2(
                fields.get("fldRef2")
        );

        request.setRu(
                fields.get("RU")
        );

        request.setFldClientAcctNo(
                fields.get("fldClientAcctNo")
        );

        request.setBankid(
                fields.get("bankid")
        );

        return request;
    }

    private Map<String, String> parseFields(String rawTransaction) {

        Map<String, String> fields = new HashMap<>();

        String[] parameters = rawTransaction.split("&", -1);

        for (String parameter : parameters) {

            if (parameter.isBlank()) {
                continue;
            }

            int separatorIndex = parameter.indexOf("=");

            if (separatorIndex <= 0) {
                continue;
            }

            String key =
                    parameter.substring(0, separatorIndex);

            String value =
                    parameter.substring(separatorIndex + 1);

            fields.put(key, value);
        }

        return fields;
    }
}