package com.example.banksimulator.service;

import com.example.banksimulator.model.PaymentRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ValidationServiceTest {

    private final ValidationService validationService =
            new ValidationService();

    private PaymentRequest validRequest() {

        PaymentRequest request = new PaymentRequest();

        request.setFldClientCode("Amazon");
        request.setFldMerchCode("Merc1");
        request.setFldTxnCurr("INR");
        request.setFldTxnAmt(new BigDecimal("999999.99"));
        request.setFldTxnScAmt(new BigDecimal("999999.99"));
        request.setFldMerchRefNbr("A123401");
        request.setFldDatTimeTxn(
                LocalDateTime.of(2018, 10, 10, 10, 10, 10)
        );
        request.setFldRef2("SUB123");
        request.setRu("https://www.billdesk.com");
        request.setFldClientAcctNo("22222");

        return request;
    }

    @Test
    void shouldAcceptValidRequest() {

        PaymentRequest request = validRequest();

        assertDoesNotThrow(
                () -> validationService.validatePaymentRequest(request)
        );
    }

    @Test
    void shouldRejectNullRequest() {

        assertThrows(
                IllegalArgumentException.class,
                () -> validationService.validatePaymentRequest(null)
        );
    }

    @Test
    void shouldRejectMissingClientCode() {

        PaymentRequest request = validRequest();
        request.setFldClientCode(null);

        assertThrows(
                IllegalArgumentException.class,
                () -> validationService.validatePaymentRequest(request)
        );
    }

    @Test
    void shouldRejectMissingMerchantCode() {

        PaymentRequest request = validRequest();
        request.setFldMerchCode(null);

        assertThrows(
                IllegalArgumentException.class,
                () -> validationService.validatePaymentRequest(request)
        );
    }

    @Test
    void shouldRejectMissingCurrency() {

        PaymentRequest request = validRequest();
        request.setFldTxnCurr(null);

        assertThrows(
                IllegalArgumentException.class,
                () -> validationService.validatePaymentRequest(request)
        );
    }

    @Test
    void shouldRejectMissingAmount() {

        PaymentRequest request = validRequest();
        request.setFldTxnAmt(null);

        assertThrows(
                IllegalArgumentException.class,
                () -> validationService.validatePaymentRequest(request)
        );
    }

    @Test
    void shouldRejectMissingServiceCharge() {

        PaymentRequest request = validRequest();
        request.setFldTxnScAmt(null);

        assertThrows(
                IllegalArgumentException.class,
                () -> validationService.validatePaymentRequest(request)
        );
    }

    @Test
    void shouldRejectMissingMerchantReference() {

        PaymentRequest request = validRequest();
        request.setFldMerchRefNbr(null);

        assertThrows(
                IllegalArgumentException.class,
                () -> validationService.validatePaymentRequest(request)
        );
    }

    @Test
    void shouldRejectMissingTransactionDateTime() {

        PaymentRequest request = validRequest();
        request.setFldDatTimeTxn(null);

        assertThrows(
                IllegalArgumentException.class,
                () -> validationService.validatePaymentRequest(request)
        );
    }

    @Test
    void shouldRejectMissingSubMerchantCode() {

        PaymentRequest request = validRequest();
        request.setFldRef2(null);

        assertThrows(
                IllegalArgumentException.class,
                () -> validationService.validatePaymentRequest(request)
        );
    }

    @Test
    void shouldRejectMissingReturnUrl() {

        PaymentRequest request = validRequest();
        request.setRu(null);

        assertThrows(
                IllegalArgumentException.class,
                () -> validationService.validatePaymentRequest(request)
        );
    }

    @Test
    void shouldRejectMissingClientAccount() {

        PaymentRequest request = validRequest();
        request.setFldClientAcctNo(null);

        assertThrows(
                IllegalArgumentException.class,
                () -> validationService.validatePaymentRequest(request)
        );
    }

    @Test
    void shouldRejectZeroTransactionAmount() {
        PaymentRequest request = validRequest();
        request.setFldTxnAmt(BigDecimal.ZERO);

        assertThrows(
                IllegalArgumentException.class,
                () -> validationService.validatePaymentRequest(request)
        );
    }

    @Test
    void shouldRejectNegativeServiceCharge() {
        PaymentRequest request = validRequest();
        request.setFldTxnScAmt(new BigDecimal("-1.00"));

        assertThrows(
                IllegalArgumentException.class,
                () -> validationService.validatePaymentRequest(request)
        );
    }


}