package com.ybm.banksimulator.service;

import com.ybm.banksimulator.model.PaymentRequest;
import com.ybm.banksimulator.model.PaymentResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class BankPaymentServiceTest {

    private final EncryptionService encryptionService =
            new EncryptionService();

    private final ChecksumService checksumService =
            new ChecksumService();

    private final EpiTransactionParser transactionParser =
            new EpiTransactionParser();

    private final EpiTransactionBuilder transactionBuilder =
            new EpiTransactionBuilder();

    private final EpiResponseBuilder epiResponseBuilder =
            new EpiResponseBuilder();

    private final DualVerificationService dualVerificationService =
            new DualVerificationService();

    private final BankPaymentService bankPaymentService =
            new BankPaymentService(
                    encryptionService,
                    checksumService,
                    transactionParser,
                    dualVerificationService,
                    epiResponseBuilder
            );

    private static final String PID =
            "PID_XYZ_001";

    private static final String KEY =
            "BILL#1234";


    /*
     * =========================================================
     * VALID REQUEST
     * =========================================================
     *
     * Total debit:
     *
     * 900,000.00
     * + 50,000.00
     * = 950,000.00
     *
     * Account balance is 1,000,000.00,
     * so SUCCESS is possible.
     */

    private PaymentRequest validRequest() {

        PaymentRequest request =
                new PaymentRequest();

        request.setFldClientCode("Amazon");

        request.setFldMerchCode("Merch1");

        request.setFldTxnCurr("INR");

        request.setFldTxnAmt(
                new BigDecimal("900000.00")
        );

        request.setFldTxnScAmt(
                new BigDecimal("50000.00")
        );

        request.setFldMerchRefNbr(
                "A123401"
        );

        request.setFldDatTimeTxn(
                LocalDateTime.of(
                        2018,
                        10,
                        10,
                        10,
                        10,
                        10
                )
        );

        request.setFldRef2("SUB123");

        request.setRu(
                "https://www.billdesk.com"
        );

        request.setFldClientAcctNo(
                "22222"
        );

        return request;
    }


    /*
     * =========================================================
     * CREATE ENCRYPTED TRANSACTION
     * =========================================================
     */

    private String createEncdata(
            PaymentRequest request) {

        String rawTransaction =
                transactionBuilder.buildRawTransaction(
                        request
                );

        String checksum =
                checksumService.generateChecksum(
                        rawTransaction
                );

        String rawWithChecksum =
                rawTransaction
                        + "&CHECKSUM="
                        + checksum;

        return encryptionService.encrypt(
                rawWithChecksum,
                KEY
        );
    }


    /*
     * =========================================================
     * SUCCESS
     * =========================================================
     */

    @Test
    void shouldProcessSuccessfulPayment() {

        PaymentRequest request =
                validRequest();

        String encdata =
                createEncdata(request);


        /*
         * Gateway → Bank
         *
         * Receive transaction first.
         */

        PaymentRequest received =
                bankPaymentService.receiveTransaction(
                        PID,
                        encdata
                );

        assertNotNull(received);


        /*
         * Bank Simulator → Process SUCCESS
         */

        PaymentResponse response =
                bankPaymentService.processSimulation(
                        "SUCCESS"
                );


        /*
         * Basic response
         */

        assertNotNull(response);


        /*
         * Transaction fields
         */

        assertEquals(
                "Amazon",
                response.getFldClientCode()
        );

        assertEquals(
                "Merch1",
                response.getFldMerchCode()
        );

        assertEquals(
                "INR",
                response.getFldTxnCurr()
        );

        assertEquals(
                new BigDecimal("900000.00"),
                response.getFldTxnAmt()
        );

        assertEquals(
                new BigDecimal("50000.00"),
                response.getFldTxnScAmt()
        );

        assertEquals(
                "A123401",
                response.getFldMerchRefNbr()
        );


        /*
         * SUCCESS
         */

        assertEquals(
                "S",
                response.getFlgSuccess()
        );


        /*
         * Bank reference must exist.
         */

        assertNotNull(
                response.getBankRefNo()
        );


        /*
         * Ref2
         */

        assertEquals(
                "SUB123",
                response.getFldRef2()
        );


        /*
         * Dual Verification must have succeeded.
         */

        assertEquals(
                "Merchant transaction successful - "
                        + "Dual Verification successful",
                response.getMessage()
        );
    }


    /*
     * =========================================================
     * INVALID ACCOUNT
     * =========================================================
     */

    @Test
    void shouldRejectUnknownAccount() {

        PaymentRequest request =
                validRequest();

        request.setFldClientAcctNo(
                "99999"
        );

        String encdata =
                createEncdata(request);


        bankPaymentService.receiveTransaction(
                PID,
                encdata
        );


        PaymentResponse response =
                bankPaymentService.processSimulation(
                        "SUCCESS"
                );


        /*
         * SUCCESS processing performs actual bank validation -
         * an unknown account must be rejected, not accepted.
         *
         * (This test previously asserted "S" here, which is
         * incorrect: it was checking that account validation
         * happened at all, not that it actually rejected an
         * invalid account. That gave a false sense of safety -
         * the test passed even though nothing was really being
         * verified.)
         */

        assertEquals(
                "F",
                response.getFlgSuccess()
        );

        assertEquals(
                "Invalid customer account",
                response.getMessage()
        );
    }


    /*
     * =========================================================
     * INVALID ACCOUNT SCENARIO
     * =========================================================
     *
     * The simulator's INVALID_ACCOUNT option
     * deliberately returns a business failure.
     */

    @Test
    void shouldSimulateInvalidAccount() {

        PaymentRequest request =
                validRequest();

        String encdata =
                createEncdata(request);


        bankPaymentService.receiveTransaction(
                PID,
                encdata
        );


        PaymentResponse response =
                bankPaymentService.processSimulation(
                        "INVALID_ACCOUNT"
                );


        assertNotNull(response);

        assertEquals(
                "F",
                response.getFlgSuccess()
        );

        assertEquals(
                "Invalid customer account",
                response.getMessage()
        );
    }


    /*
     * =========================================================
     * INVALID PID
     * =========================================================
     *
     * The simulator allows the transaction to arrive
     * with an intentionally wrong PID.
     *
     * The SUCCESS path then rejects it through
     * validateBankRules().
     */

    @Test
    void shouldRejectInvalidPid() {

        PaymentRequest request =
                validRequest();

        String encdata =
                createEncdata(request);


        bankPaymentService.receiveTransaction(
                "INVALID_PID",
                encdata
        );


        PaymentResponse response =
                bankPaymentService.processSimulation(
                        "SUCCESS"
                );


        assertEquals(
                "F",
                response.getFlgSuccess()
        );

        assertEquals(
                "Invalid PID",
                response.getMessage()
        );
    }


    /*
     * =========================================================
     * INVALID CHECKSUM
     * =========================================================
     */

    @Test
    void shouldRejectInvalidChecksum() {

        PaymentRequest request =
                validRequest();

        String rawTransaction =
                transactionBuilder.buildRawTransaction(
                        request
                );

        String invalidData =
                rawTransaction
                        + "&CHECKSUM="
                        + "00000000000000000000000000000000";

        String encdata =
                encryptionService.encrypt(
                        invalidData,
                        KEY
                );


        bankPaymentService.receiveTransaction(
                PID,
                encdata
        );


        PaymentResponse response =
                bankPaymentService.processSimulation(
                        "SUCCESS"
                );


        assertEquals(
                "F",
                response.getFlgSuccess()
        );

        assertEquals(
                "Checksum verification failed",
                response.getMessage()
        );
    }


    /*
     * =========================================================
     * INVALID CHECKSUM SCENARIO
     * =========================================================
     *
     * This tests the Bank Simulator dropdown option,
     * not the actual checksum validation.
     */

    @Test
    void shouldSimulateInvalidChecksum() {

        PaymentRequest request =
                validRequest();

        String encdata =
                createEncdata(request);


        bankPaymentService.receiveTransaction(
                PID,
                encdata
        );


        PaymentResponse response =
                bankPaymentService.processSimulation(
                        "INVALID_CHECKSUM"
                );


        assertEquals(
                "F",
                response.getFlgSuccess()
        );

        assertEquals(
                "Checksum verification failed",
                response.getMessage()
        );
    }


    /*
     * =========================================================
     * INSUFFICIENT FUNDS
     * =========================================================
     */

    @Test
    void shouldRejectInsufficientFunds() {

        PaymentRequest request =
                validRequest();

        request.setFldTxnAmt(
                new BigDecimal("950000.00")
        );

        request.setFldTxnScAmt(
                new BigDecimal("100000.00")
        );


        String encdata =
                createEncdata(request);


        bankPaymentService.receiveTransaction(
                PID,
                encdata
        );


        PaymentResponse response =
                bankPaymentService.processSimulation(
                        "SUCCESS"
                );


        assertEquals(
                "F",
                response.getFlgSuccess()
        );

        assertEquals(
                "Insufficient funds",
                response.getMessage()
        );
    }


    /*
     * =========================================================
     * INVALID CURRENCY
     * =========================================================
     */

    @Test
    void shouldRejectInvalidCurrency() {

        PaymentRequest request =
                validRequest();

        request.setFldTxnCurr(
                "USD"
        );

        String encdata =
                createEncdata(request);


        bankPaymentService.receiveTransaction(
                PID,
                encdata
        );


        PaymentResponse response =
                bankPaymentService.processSimulation(
                        "SUCCESS"
                );


        assertEquals(
                "F",
                response.getFlgSuccess()
        );

        assertEquals(
                "Unsupported transaction currency",
                response.getMessage()
        );
    }


    /*
     * =========================================================
     * WRONG ENCRYPTION KEY
     * =========================================================
     *
     * Encryption/decryption failure happens during
     * receiveTransaction(), before the transaction
     * reaches the simulator processing stage.
     */

    @Test
    void shouldRejectWrongEncryptionKeyData() {

        PaymentRequest request =
                validRequest();

        String rawTransaction =
                transactionBuilder.buildRawTransaction(
                        request
                );

        String checksum =
                checksumService.generateChecksum(
                        rawTransaction
                );

        String rawWithChecksum =
                rawTransaction
                        + "&CHECKSUM="
                        + checksum;

        String encdata =
                encryptionService.encrypt(
                        rawWithChecksum,
                        "WRONG_KEY"
                );


        assertThrows(
                RuntimeException.class,
                () ->
                        bankPaymentService.receiveTransaction(
                                PID,
                                encdata
                        )
        );
    }


    /*
     * =========================================================
     * PER-TRANSACTION LIMIT
     * =========================================================
     *
     * A single transaction whose principal amount alone exceeds
     * the per-transaction limit must be rejected, even though
     * the account balance (1,000,000.00) could easily cover it.
     */

    @Test
    void shouldRejectWhenPerTransactionLimitExceeded() {

        PaymentRequest request =
                validRequest();

        request.setFldTxnAmt(
                new BigDecimal("960000.00")
        );

        request.setFldTxnScAmt(
                new BigDecimal("0.00")
        );

        String encdata =
                createEncdata(request);

        bankPaymentService.receiveTransaction(
                PID,
                encdata
        );

        PaymentResponse response =
                bankPaymentService.processSimulation(
                        "SUCCESS"
                );

        assertEquals(
                "F",
                response.getFlgSuccess()
        );

        assertEquals(
                "Transaction amount exceeds per-transaction limit",
                response.getMessage()
        );
    }


    /*
     * =========================================================
     * DAILY LIMIT
     * =========================================================
     *
     * Two separate transactions, each individually well within
     * balance and within the per-transaction limit, but whose
     * CUMULATIVE total crosses the daily limit. This is what
     * distinguishes a daily limit from a balance check - the
     * second transaction is declined purely because of what
     * already happened earlier the same day, not because of
     * anything wrong with the second transaction itself.
     */

    @Test
    void shouldRejectWhenDailyLimitExceeded() {

        PaymentRequest firstRequest =
                validRequest();

        String firstEncdata =
                createEncdata(firstRequest);

        bankPaymentService.receiveTransaction(
                PID,
                firstEncdata
        );

        PaymentResponse firstResponse =
                bankPaymentService.processSimulation(
                        "SUCCESS"
                );

        assertEquals(
                "S",
                firstResponse.getFlgSuccess()
        );

        /*
         * Second transaction: different merchant reference
         * (so it isn't caught by the duplicate guard instead),
         * small amount, well within the remaining balance -
         * but pushes the day's cumulative total over the limit.
         */

        PaymentRequest secondRequest =
                validRequest();

        secondRequest.setFldMerchRefNbr(
                "A123402"
        );

        secondRequest.setFldTxnAmt(
                new BigDecimal("5000.00")
        );

        secondRequest.setFldTxnScAmt(
                new BigDecimal("0.00")
        );

        String secondEncdata =
                createEncdata(secondRequest);

        bankPaymentService.receiveTransaction(
                PID,
                secondEncdata
        );

        PaymentResponse secondResponse =
                bankPaymentService.processSimulation(
                        "SUCCESS"
                );

        assertEquals(
                "F",
                secondResponse.getFlgSuccess()
        );

        assertEquals(
                "Daily transaction limit exceeded",
                secondResponse.getMessage()
        );
    }


    /*
     * =========================================================
     * DUPLICATE TRANSACTION
     * =========================================================
     *
     * Once a merchant reference has been successfully processed,
     * the bank must refuse to process it again - even in a brand
     * new incoming transaction with a valid checksum and PID.
     */

    @Test
    void shouldRejectDuplicateMerchantReference() {

        PaymentRequest request =
                validRequest();

        String encdata =
                createEncdata(request);

        bankPaymentService.receiveTransaction(
                PID,
                encdata
        );

        PaymentResponse firstResponse =
                bankPaymentService.processSimulation(
                        "SUCCESS"
                );

        assertEquals(
                "S",
                firstResponse.getFlgSuccess()
        );

        /*
         * Same merchant reference ("A123401" from validRequest())
         * submitted again as a brand new transaction.
         */

        PaymentRequest duplicateRequest =
                validRequest();

        String duplicateEncdata =
                createEncdata(duplicateRequest);

        bankPaymentService.receiveTransaction(
                PID,
                duplicateEncdata
        );

        PaymentResponse duplicateResponse =
                bankPaymentService.processSimulation(
                        "SUCCESS"
                );

        assertEquals(
                "F",
                duplicateResponse.getFlgSuccess()
        );

        assertEquals(
                "Duplicate transaction - merchant reference "
                        + "already processed",
                duplicateResponse.getMessage()
        );
    }
}