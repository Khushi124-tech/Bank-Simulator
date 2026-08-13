package com.example.banksimulator.service;

import com.example.banksimulator.model.PaymentRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EpiTransactionParserTest {

    private final EpiTransactionParser parser =
            new EpiTransactionParser();

    @Test
    void shouldParseValidTransaction() {

        String rawTransaction =
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

        PaymentRequest request =
                parser.parse(rawTransaction);

        assertEquals("Amazon", request.getFldClientCode());
        assertEquals("Merch1", request.getFldMerchCode());
        assertEquals("INR", request.getFldTxnCurr());

        assertEquals(
                new BigDecimal("999999.99"),
                request.getFldTxnAmt()
        );

        assertEquals(
                new BigDecimal("999999.99"),
                request.getFldTxnScAmt()
        );

        assertEquals(
                "A123401",
                request.getFldMerchRefNbr()
        );

        assertEquals(
                LocalDateTime.of(2018, 10, 10, 10, 10, 10),
                request.getFldDatTimeTxn()
        );

        assertEquals("SUB123", request.getFldRef2());
        assertEquals("https://www.billdesk.com", request.getRu());
        assertEquals("22222", request.getFldClientAcctNo());
    }

    @Test
    void shouldRejectNullTransaction() {

        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse(null)
        );
    }

    @Test
    void shouldRejectBlankTransaction() {

        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse("   ")
        );
    }

    @Test
    void shouldParseEmptyOptionalFields() {

        String rawTransaction =
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

        PaymentRequest request =
                parser.parse(rawTransaction);

        assertNull(request.getFldRef1());
        assertEquals("SUB123", request.getFldRef2());
    }
}