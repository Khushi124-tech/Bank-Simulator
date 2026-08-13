package com.example.banksimulator.service;

import com.example.banksimulator.model.PaymentRequest;
import com.example.banksimulator.model.PaymentResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class BankPaymentService {

    private final EncryptionService encryptionService;
    private final ChecksumService checksumService;
    private final EpiTransactionParser transactionParser;
    private final DualVerificationService dualVerificationService;

    private static final String BANK_PID = "PID_XYZ_001";
    private static final String ENCRYPTION_KEY = "BILL#1234";

    private static final String DEMO_ACCOUNT = "22222";

    private static long bankReferenceSequence = 1;

    private static BigDecimal accountBalance =
            new BigDecimal("1000000.00");

    /*
     * =========================================================
     * PENDING TRANSACTION
     * =========================================================
     *
     * The bank simulator keeps the most recently received
     * transaction here until the tester chooses a scenario.
     */

    private PaymentRequest pendingTransaction;

    private String pendingPid;

    private boolean pendingChecksumValid;

    public BankPaymentService(
            EncryptionService encryptionService,
            ChecksumService checksumService,
            EpiTransactionParser transactionParser,
            DualVerificationService dualVerificationService) {

        this.encryptionService = encryptionService;
        this.checksumService = checksumService;
        this.transactionParser = transactionParser;
        this.dualVerificationService = dualVerificationService;
    }

    /*
     * =========================================================
     * RECEIVE TRANSACTION
     * =========================================================
     *
     * Gateway sends PID + encdata to the bank.
     *
     * IMPORTANT:
     * This method DOES NOT debit the account.
     *
     * It only:
     *
     * 1. Validates that PID exists
     * 2. Decrypts encdata
     * 3. Extracts checksum
     * 4. Verifies checksum
     * 5. Parses the transaction
     * 6. Stores it as a pending transaction
     */
    public synchronized PaymentRequest receiveTransaction(
            String pid,
            String encdata) {

        if (pid == null || pid.isBlank()) {
            throw new IllegalArgumentException(
                    "PID cannot be empty"
            );
        }

        if (encdata == null || encdata.isBlank()) {
            throw new IllegalArgumentException(
                    "encdata cannot be empty"
            );
        }

        /*
         * Store PID because the Bank Simulator UI can later
         * deliberately simulate INVALID_PID.
         */
        this.pendingPid = pid;

        /*
         * Decrypt.
         */
        String decryptedData =
                encryptionService.decrypt(
                        encdata,
                        ENCRYPTION_KEY
                );

        System.out.println("Decrypted data:");
        System.out.println(decryptedData);

        /*
         * Extract transaction and checksum.
         *
         * We DON'T immediately reject an invalid checksum.
         *
         * The simulator needs to allow the tester to choose
         * INVALID_CHECKSUM from the Bank UI.
         */
        int checksumIndex =
                decryptedData.lastIndexOf("&CHECKSUM=");

        if (checksumIndex == -1) {
            throw new IllegalArgumentException(
                    "CHECKSUM not found"
            );
        }

        String rawTransaction =
                decryptedData.substring(
                        0,
                        checksumIndex
                );

        String receivedChecksum =
                decryptedData.substring(
                        checksumIndex
                                + "&CHECKSUM=".length()
                );

        if (receivedChecksum.isBlank()) {
            throw new IllegalArgumentException(
                    "CHECKSUM is empty"
            );
        }

        String calculatedChecksum =
                checksumService.generateChecksum(
                        rawTransaction
                );

        this.pendingChecksumValid =
                calculatedChecksum.equalsIgnoreCase(
                        receivedChecksum
                );

        if (pendingChecksumValid) {

            System.out.println(
                    "Checksum verification successful"
            );

        } else {

            System.out.println(
                    "Checksum verification failed"
            );
        }

        /*
         * Parse the transaction.
         */
        PaymentRequest request =
                transactionParser.parse(
                        rawTransaction
                );

        /*
         * Store as pending.
         */
        this.pendingTransaction = request;

        System.out.println(
                "Transaction received by Bank Simulator"
        );

        System.out.println(
                "PID: " + pid
        );

        System.out.println(
                "Account: "
                        + request.getFldClientAcctNo()
        );

        System.out.println(
                "Transaction Amount: ₹"
                        + request.getFldTxnAmt()
        );

        System.out.println(
                "Service Amount: ₹"
                        + request.getFldTxnScAmt()
        );

        System.out.println(
                "Total Debit: ₹"
                        + calculateTotalDebit(request)
        );

        return request;
    }

    /*
     * =========================================================
     * GET PENDING TRANSACTION
     * =========================================================
     *
     * Bank Simulator UI calls this to display the transaction.
     */
    public synchronized PaymentRequest getPendingTransaction() {

        if (pendingTransaction == null) {
            throw new IllegalStateException(
                    "No pending transaction"
            );
        }

        return pendingTransaction;
    }
    public synchronized String getPendingPid() {
        return pendingPid;
    }

    public synchronized boolean isPendingChecksumValid() {
        return pendingChecksumValid;
    }

    /*
     * =========================================================
     * PROCESS SIMULATION
     * =========================================================
     *
     * Called after the Bank Simulator tester chooses:
     *
     * SUCCESS
     * INVALID_ACCOUNT
     * INSUFFICIENT_FUNDS
     * INVALID_PID
     * INVALID_CHECKSUM
     * INVALID_CURRENCY
     */
    public synchronized PaymentResponse processSimulation(
            String scenario) {

        if (pendingTransaction == null) {
            throw new IllegalStateException(
                    "No pending transaction"
            );
        }

        if (scenario == null || scenario.isBlank()) {
            throw new IllegalArgumentException(
                    "Simulation scenario is required"
            );
        }

        switch (scenario.toUpperCase()) {

            case "SUCCESS":

                return processSuccess();

            case "INVALID_ACCOUNT":

                return createFailureResponse(
                        "Invalid customer account"
                );

            case "INSUFFICIENT_FUNDS":

                return createFailureResponse(
                        "Insufficient funds"
                );

            case "INVALID_PID":

                return createFailureResponse(
                        "Invalid PID"
                );

            case "INVALID_CHECKSUM":

                return createFailureResponse(
                        "Checksum verification failed"
                );

            case "INVALID_CURRENCY":

                return createFailureResponse(
                        "Unsupported transaction currency"
                );

            default:

                throw new IllegalArgumentException(
                        "Unknown simulation scenario: "
                                + scenario
                );
        }
    }

    /*
     * =========================================================
     * SUCCESS
     * =========================================================
     */
    private PaymentResponse processSuccess() {

        PaymentRequest request =
                pendingTransaction;

        /*
         * For an actual SUCCESS simulation,
         * perform the real bank validations.
         */

        validateBankRules(request);

        BigDecimal totalDebit =
                calculateTotalDebit(request);
        /*
         * Debit account.
         */
        accountBalance =
                accountBalance.subtract(totalDebit);

        /*
         * Generate bank reference.
         */
        String bankReferenceNumber =
                String.valueOf(
                        551151025056L
                                + bankReferenceSequence++
                );

        System.out.println(
                "Payment successful"
        );

        System.out.println(
                "Account: "
                        + DEMO_ACCOUNT
        );

        System.out.println(
                "Transaction Amount: ₹"
                        + request.getFldTxnAmt()
        );

        System.out.println(
                "Service Amount: ₹"
                        + request.getFldTxnScAmt()
        );

        System.out.println(
                "Total Debited: ₹"
                        + totalDebit
        );

        System.out.println(
                "Remaining balance: ₹"
                        + accountBalance
        );

        System.out.println(
                "Bank Reference Number: "
                        + bankReferenceNumber
        );

        /*
         * Build response.
         */
        PaymentResponse response =
                new PaymentResponse();

        response.setFldClientCode(
                request.getFldClientCode()
        );

        response.setFldMerchCode(
                request.getFldMerchCode()
        );

        response.setFldTxnCurr(
                request.getFldTxnCurr()
        );

        response.setFldTxnAmt(
                request.getFldTxnAmt()
        );

        response.setFldTxnScAmt(
                request.getFldTxnScAmt()
        );

        response.setFldMerchRefNbr(
                request.getFldMerchRefNbr()
        );

        response.setFlgSuccess("S");

        response.setFldDatTimeTxn(
                request.getFldDatTimeTxn()
        );

        response.setFldRef2(
                request.getFldRef2()
        );

        response.setBankRefNo(
                bankReferenceNumber
        );

        response.setMessage(
                "Merchant transaction successful"
        );

        /*
         * =========================================================
         * REGISTER COMPLETED TRANSACTION
         * =========================================================
         */

        dualVerificationService.registerCompletedTransaction(
                response
        );


        /*
         * =========================================================
         * DUAL VERIFICATION
         * =========================================================
         *
         * The Bank verifies the transaction against the
         * transaction it actually completed.
         */

        var verificationResult =
                dualVerificationService.verify(
                        response
                );


        /*
         * =========================================================
         * VERIFICATION FAILED
         * =========================================================
         */

        if (!verificationResult.isVerified()) {

            response.setFlgSuccess("F");

            response.setFldVerify("N");

            response.setMessage(
                    verificationResult.getMessage()
            );

            clearPendingTransaction();

            return response;
        }





        /*
         * =========================================================
         * VERIFICATION FAILED
         * =========================================================
         *
         * Do not allow the UI to show SUCCESS if verification
         * fails.
         */

        if (!verificationResult.isVerified()) {

            response.setFlgSuccess("F");

            response.setMessage(
                    verificationResult.getMessage()
            );

            clearPendingTransaction();

            return response;
        }


        /*
         * =========================================================
         * VERIFICATION SUCCESSFUL
         * =========================================================
         */

        response.setMessage(
                "Merchant transaction successful - "
                        + "Dual Verification successful"
        );

        /*
         * Transaction is finished.
         */
        clearPendingTransaction();

        return response;
    }

    /*
     * =========================================================
     * FAILURE RESPONSE
     * =========================================================
     */
    private PaymentResponse createFailureResponse(
            String message) {

        PaymentRequest request =
                pendingTransaction;

        PaymentResponse response =
                new PaymentResponse();

        response.setFldClientCode(
                request.getFldClientCode()
        );

        response.setFldMerchCode(
                request.getFldMerchCode()
        );

        response.setFldTxnCurr(
                request.getFldTxnCurr()
        );

        response.setFldTxnAmt(
                request.getFldTxnAmt()
        );

        response.setFldTxnScAmt(
                request.getFldTxnScAmt()
        );

        response.setFldMerchRefNbr(
                request.getFldMerchRefNbr()
        );

        /*
         * F = Failure
         */
        response.setFlgSuccess("F");

        response.setFldDatTimeTxn(
                request.getFldDatTimeTxn()
        );

        response.setFldRef2(
                request.getFldRef2()
        );

        /*
         * Failed transaction has no bank reference.
         */
        response.setBankRefNo(null);

        response.setMessage(message);

        System.out.println(
                "Payment result: "
                        + message
        );

        /*
         * Transaction is finished.
         */
        clearPendingTransaction();

        return response;
    }

    /*
     * =========================================================
     * TOTAL DEBIT
     * =========================================================
     *
     * The customer's account is debited for:
     *
     * Transaction Amount
     * +
     * Service Amount
     */

    private BigDecimal calculateTotalDebit(
            PaymentRequest request) {

        BigDecimal txnAmount =
                request.getFldTxnAmt() != null
                        ? request.getFldTxnAmt()
                        : BigDecimal.ZERO;

        BigDecimal serviceAmount =
                request.getFldTxnScAmt() != null
                        ? request.getFldTxnScAmt()
                        : BigDecimal.ZERO;
        return txnAmount.add(serviceAmount);
    }
        /*
     * =========================================================
     * BANK BUSINESS VALIDATION
     * =========================================================
     */
    private void validateBankRules(
            PaymentRequest request) {

        if (request == null) {

            throw new IllegalArgumentException(
                    "Payment request cannot be null"
            );
        }

        /*
         * Account
         */
        if (!DEMO_ACCOUNT.equals(
                request.getFldClientAcctNo())) {

            throw new IllegalArgumentException(
                    "Invalid customer account"
            );
        }

        /*
         * Amount
         */
        if (request.getFldTxnAmt() == null ||
                request.getFldTxnAmt()
                        .compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "Transaction amount must be greater than zero"
            );
        }

        /*
         * Currency
         */
        if (request.getFldTxnCurr() == null ||
                !"INR".equalsIgnoreCase(
                        request.getFldTxnCurr())) {

            throw new IllegalArgumentException(
                    "Unsupported transaction currency"
            );
        }

        /*
         * Merchant
         */
        if (request.getFldMerchCode() == null ||
                request.getFldMerchCode().isBlank()) {

            throw new IllegalArgumentException(
                    "Merchant code is required"
            );
        }

        /*
         * Merchant reference
         */
        if (request.getFldMerchRefNbr() == null ||
                request.getFldMerchRefNbr().isBlank()) {

            throw new IllegalArgumentException(
                    "Merchant reference number is required"
            );
        }

        /*
         * PID
         */
        if (!BANK_PID.equals(pendingPid)) {

            throw new IllegalArgumentException(
                    "Invalid PID"
            );
        }

        /*
         * CHECKSUM
         */
        if (!pendingChecksumValid) {

            throw new IllegalArgumentException(
                    "Checksum verification failed"
            );
        }

        /*
         * Balance
         */
        BigDecimal totalDebit =
                calculateTotalDebit(request);

        if (totalDebit.compareTo(accountBalance) > 0) {

            throw new IllegalArgumentException(
                    "Insufficient funds"
            );
        }
    }

    /*
     * =========================================================
     * CLEAR PENDING TRANSACTION
     * =========================================================
     */
    private void clearPendingTransaction() {

        pendingTransaction = null;
        pendingPid = null;
        pendingChecksumValid = false;
    }

    /*
     * =========================================================
     * ACCOUNT BALANCE
     * =========================================================
     */
    public BigDecimal getAccountBalance() {

        return accountBalance;
    }
}