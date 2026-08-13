package com.example.banksimulator.service;

import com.example.banksimulator.model.PaymentResponse;
import com.example.banksimulator.service.EpiResponseBuilder;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EpiResponseBuilderTest {

    private final EpiResponseBuilder responseBuilder =
            new EpiResponseBuilder();

    private PaymentResponse validResponse() {

        PaymentResponse response = new PaymentResponse();

        response.setFldClientCode("Amazon");
        response.setFldMerchCode("Merch1");
        response.setFldTxnCurr("INR");
        response.setFldTxnAmt(new BigDecimal("999999.99"));
        response.setFldTxnScAmt(new BigDecimal("999999.99"));
        response.setFldMerchRefNbr("A123401");
        response.setFlgSuccess("S");

        response.setFldDatTimeTxn(
                LocalDateTime.of(2026, 8, 11, 16, 30, 0)
        );

        response.setBankRefNo("551151025056");
        response.setFldRef2("SUB123");
        response.setMessage(
                "Merchant transaction successful"
        );

        return response;
    }

    @Test
    void shouldBuildSuccessfulResponse() {

        PaymentResponse response = validResponse();

        String rawResponse =
                responseBuilder.buildRawResponse(response);

        assertEquals(
                "fldClientCode=Amazon" +
                        "&fldMerchCode=Merch1" +
                        "&fldTxnCurr=INR" +
                        "&fldTxnAmt=999999.99" +
                        "&fldTxnScAmt=999999.99" +
                        "&fldMerchRefNbr=A123401" +
                        "&flgSuccess=S" +
                        "&fldDatTimeTxn=11/08/2026 16:30:00" +
                        "&BankRefNo=551151025056" +
                        "&fldRef2=SUB123" +
                        "&Message=Merchant transaction successful",
                rawResponse
        );
    }

    @Test
    void shouldBuildFailedResponse() {

        PaymentResponse response = validResponse();

        response.setFlgSuccess("F");
        response.setBankRefNo("0");
        response.setMessage(
                "Merchant transaction failed"
        );

        String rawResponse =
                responseBuilder.buildRawResponse(response);

        assertTrue(rawResponse.contains("flgSuccess=F"));
        assertTrue(rawResponse.contains("BankRefNo=0"));
        assertTrue(
                rawResponse.contains(
                        "Message=Merchant transaction failed"
                )
        );
    }

    @Test
    void shouldRejectNullResponse() {

        assertThrows(
                IllegalArgumentException.class,
                () -> responseBuilder.buildRawResponse(null)
        );
    }
}
