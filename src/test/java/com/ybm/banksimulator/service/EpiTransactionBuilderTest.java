package com.ybm.banksimulator.service;

import com.ybm.banksimulator.model.PaymentRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EpiTransactionBuilderTest {

    private final EpiTransactionBuilder builder =
            new EpiTransactionBuilder();

    private PaymentRequest validRequest() {

        PaymentRequest request = new PaymentRequest();

        request.setFldClientCode("Amazon");
        request.setFldMerchCode("Merc1");
        request.setFldTxnCurr("INR");
        request.setFldTxnAmt(new BigDecimal("999999.99"));
        request.setFldTxnScAmt(new BigDecimal("0"));
        request.setFldMerchRefNbr("A123401");

        request.setFldDatTimeTxn(
                LocalDateTime.of(2018, 10, 10, 10, 10, 10)
        );

        request.setFldRef1("");
        request.setFldRef2("INSUB01");
        request.setFldRef3("");
        request.setFldRef4("");
        request.setFldRef5("");
        request.setFldRef6("");
        request.setFldRef7("");
        request.setFldRef8("");
        request.setFldRef9("");
        request.setFldRef10("");
        request.setFldRef11("");

        request.setFldDate1("");
        request.setFldDate2("");

        request.setRu("https://www.billdesk.com");
        request.setFldClientAcctNo("22222");

        return request;
    }

    @Test
    void shouldBuildRawTransaction() {

        PaymentRequest request = validRequest();

        String result =
                builder.buildRawTransaction(request);

        assertNotNull(result);

        assertTrue(
                result.contains("fldClientCode=Amazon")
        );

        assertTrue(
                result.contains("fldMerchCode=Merch1")
        );

        assertTrue(
                result.contains("fldTxnCurr=INR")
        );

        assertTrue(
                result.contains("fldTxnAmt=999999.99")
        );

        assertTrue(
                result.contains("fldTxnScAmt=999999.99")
        );

        assertTrue(
                result.contains("fldMerchRefNbr=A123401")
        );
    }

    @Test
    void shouldFormatDateTimeCorrectly() {

        String result =
                builder.buildRawTransaction(
                        validRequest()
                );

        assertTrue(
                result.contains(
                        "fldDatTimeTxn=10/10/2018 10:10:10"
                )
        );
    }

    @Test
    void shouldIncludeOptionalFieldsAsEmpty() {

        PaymentRequest request = validRequest();

        request.setFldRef1(null);
        request.setFldRef3(null);
        request.setFldRef4(null);
        request.setFldRef5(null);
        request.setFldRef6(null);
        request.setFldRef7(null);
        request.setFldRef8(null);
        request.setFldRef9(null);
        request.setFldRef10(null);
        request.setFldRef11(null);

        request.setFldDate1(null);
        request.setFldDate2(null);

        String result =
                builder.buildRawTransaction(request);

        assertTrue(result.contains("&fldRef1=&"));
        assertTrue(result.contains("&fldRef3=&"));
        assertTrue(result.contains("&fldRef11=&"));
        assertTrue(result.contains("&fldDate1=&"));
        assertTrue(result.contains("&fldDate2=&"));
    }

    @Test
    void shouldIncludeReturnUrlAndClientAccount() {

        String result =
                builder.buildRawTransaction(
                        validRequest()
                );

        assertTrue(
                result.endsWith(
                        "&RU=https://www.billdesk.com&fldClientAcctNo=22222"
                )
        );
    }

    @Test
    void shouldPreserveFieldOrder() {

        String result =
                builder.buildRawTransaction(
                        validRequest()
                );

        assertTrue(
                result.indexOf("fldClientCode=")
                        <
                        result.indexOf("fldMerchCode=")
        );

        assertTrue(
                result.indexOf("fldMerchCode=")
                        <
                        result.indexOf("fldTxnCurr=")
        );

        assertTrue(
                result.indexOf("fldTxnCurr=")
                        <
                        result.indexOf("fldTxnAmt=")
        );

        assertTrue(
                result.indexOf("fldTxnAmt=")
                        <
                        result.indexOf("fldMerchRefNbr=")
        );
    }

    @Test
    void shouldBuildCompleteExpectedTransaction() {

        String expected =
                "fldClientCode=Amazon" +
                        "&fldMerchCode=Merch1" +
                        "&fldTxnCurr=INR" +
                        "&fldTxnAmt=999999.99" +
                        "&fldTxnScAmt=999999.99" +
                        "&fldMerchRefNbr=A123401" +
                        "&fldDatTimeTxn=10/10/2018 10:10:10" +
                        "&fldRef1=" +
                        "&fldRef2=SUB123" +
                        "&fldRef3=" +
                        "&fldRef4=" +
                        "&fldRef5=" +
                        "&fldRef6=" +
                        "&fldRef7=" +
                        "&fldRef8=" +
                        "&fldRef9=" +
                        "&fldRef10=" +
                        "&fldRef11=" +
                        "&fldDate1=" +
                        "&fldDate2=" +
                        "&RU=https://www.billdesk.com" +
                        "&fldClientAcctNo=22222";

        String actual =
                builder.buildRawTransaction(
                        validRequest()
                );

        assertEquals(expected, actual);
    }
}