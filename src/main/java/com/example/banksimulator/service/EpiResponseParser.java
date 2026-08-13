package com.example.banksimulator.service;

import com.example.banksimulator.model.PaymentResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class EpiResponseParser {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public PaymentResponse parse(String rawResponse) {

        if (rawResponse == null || rawResponse.isBlank()) {
            throw new IllegalArgumentException(
                    "Raw response cannot be empty"
            );
        }

        Map<String, String> fields =
                parseFields(rawResponse);

        PaymentResponse response =
                new PaymentResponse();

        response.setFldClientCode(
                fields.get("fldClientCode")
        );

        response.setFldMerchCode(
                fields.get("fldMerchCode")
        );

        response.setFldTxnCurr(
                fields.get("fldTxnCurr")
        );

        response.setFldTxnAmt(
                new BigDecimal(fields.get("fldTxnAmt"))
        );

        response.setFldTxnScAmt(
                new BigDecimal(fields.get("fldTxnScAmt"))
        );

        response.setFldMerchRefNbr(
                fields.get("fldMerchRefNbr")
        );

        response.setFlgSuccess(
                fields.get("flgSuccess")
        );

        response.setFldVerify(
                fields.get("fldVerify")
        );

        response.setFldDatTimeTxn(
                LocalDateTime.parse(
                        fields.get("fldDatTimeTxn"),
                        DATE_TIME_FORMATTER
                )
        );

        response.setBankRefNo(
                fields.get("BankRefNo")
        );

        response.setFldRef2(
                fields.get("fldRef2")
        );

        response.setMessage(
                fields.get("Message")
        );

        return response;
    }

    private Map<String, String> parseFields(
            String rawResponse) {

        Map<String, String> fields =
                new HashMap<>();

        String[] parameters =
                rawResponse.split("&", -1);

        for (String parameter : parameters) {

            if (parameter.isBlank()) {
                continue;
            }

            int separatorIndex =
                    parameter.indexOf("=");

            if (separatorIndex <= 0) {
                continue;
            }

            String key =
                    parameter.substring(
                            0,
                            separatorIndex
                    );

            String value =
                    parameter.substring(
                            separatorIndex + 1
                    );

            fields.put(key, value);
        }

        return fields;
    }
}