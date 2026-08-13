package com.example.banksimulator.service;

import com.example.banksimulator.model.PaymentRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class ValidationService {

    public void validatePaymentRequest(
            PaymentRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Payment request cannot be null"
            );
        }

        if (request.getFldClientCode() == null ||
                request.getFldClientCode().isBlank()) {

            throw new IllegalArgumentException(
                    "fldClientCode is mandatory"
            );
        }

        if (request.getFldMerchCode() == null ||
                request.getFldMerchCode().isBlank()) {

            throw new IllegalArgumentException(
                    "fldMerchCode is mandatory"
            );
        }

        if (request.getFldTxnCurr() == null ||
                request.getFldTxnCurr().isBlank()) {

            throw new IllegalArgumentException(
                    "fldTxnCurr is mandatory"
            );
        }

        if (request.getFldTxnAmt() == null) {

            throw new IllegalArgumentException(
                    "fldTxnAmt is mandatory"
            );
        }

        if (request.getFldTxnScAmt() == null) {

            throw new IllegalArgumentException(
                    "fldTxnScAmt is mandatory"
            );
        }

        if (request.getFldMerchRefNbr() == null ||
                request.getFldMerchRefNbr().isBlank()) {

            throw new IllegalArgumentException(
                    "fldMerchRefNbr is mandatory"
            );
        }

        if (request.getFldDatTimeTxn() == null) {

            throw new IllegalArgumentException(
                    "fldDatTimeTxn is mandatory"
            );
        }

        if (request.getFldRef2() == null ||
                request.getFldRef2().isBlank()) {

            throw new IllegalArgumentException(
                    "fldRef2 is mandatory"
            );
        }

        if (request.getRu() == null ||
                request.getRu().isBlank()) {

            throw new IllegalArgumentException(
                    "RU is mandatory"
            );
        }

        if (request.getFldClientAcctNo() == null ||
                request.getFldClientAcctNo().isBlank()) {

            throw new IllegalArgumentException(
                    "fldClientAcctNo is mandatory"
            );
        }


        /*
         * =====================================================
         * AMOUNT VALIDATION
         * =====================================================
         */

        if (request.getFldTxnAmt()
                .compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "Transaction amount must be greater than zero"
            );
        }


        if (request.getFldTxnScAmt()
                .compareTo(BigDecimal.ZERO) < 0) {

            throw new IllegalArgumentException(
                    "Service amount cannot be negative"
            );
        }


        /*
         * =====================================================
         * SERVICE AMOUNT CANNOT EXCEED TRANSACTION AMOUNT
         * =====================================================
         */

        if (request.getFldTxnScAmt()
                .compareTo(request.getFldTxnAmt()) > 0) {

            throw new IllegalArgumentException(
                    "Service amount cannot be greater than transaction amount"
            );
        }
    }
}