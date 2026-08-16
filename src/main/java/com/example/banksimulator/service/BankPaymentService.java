package com.example.banksimulator.service;

import com.example.banksimulator.model.PaymentRequest;
import com.example.banksimulator.model.PaymentResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Service
public class BankPaymentService {

    private final EncryptionService encryptionService;
    private final ChecksumService checksumService;
    private final EpiTransactionParser transactionParser;
    private final DualVerificationService dualVerificationService;
    private final EpiResponseBuilder epiResponseBuilder;

    private static final String BANK_PID = "PID_XYZ_001";
    private static final String ENCRYPTION_KEY = "BILL#1234";

    private static final String DEMO_ACCOUNT = "22222";

    private static long bankReferenceSequence = 1;

    private static BigDecimal accountBalance =
            new BigDecimal("1000000.00");

    /*
     * =========================================================
     * LIMITS
     * =========================================================
     *
     * Real banks enforce limits independently of the account's
     * actual balance - a well-funded account can still be
     * declined for exceeding one of these.
     *
     *   PER_TRANSACTION_LIMIT - caps a single transaction's
     *     principal amount, regardless of balance.
     *
     *   DAILY_TRANSACTION_LIMIT - caps the CUMULATIVE amount
     *     debited from this account across all transactions
     *     processed since the bank simulator started (a
     *     simplified stand-in for "since midnight" in a real
     *     core banking system).
     */
    private static final BigDecimal PER_TRANSACTION_LIMIT =
            new BigDecimal("950000.00");

    private static final BigDecimal DAILY_TRANSACTION_LIMIT =
            new BigDecimal("950000.00");

    /*
     * Cumulative total debited from the demo account so far.
     * Instance-level (not static) so it mirrors real deployment
     * behaviour: one Spring-managed BankPaymentService bean for
     * the whole running application, reset only on restart.
     */
    private BigDecimal dailyDebitedTotal = BigDecimal.ZERO;

    /*
     * =========================================================
     * DUPLICATE TRANSACTION / IDEMPOTENCY GUARD
     * =========================================================
     *
     * Real banks refuse to debit an account twice for the same
     * merchant transaction reference. This guards against
     * accidental retries (double-click, network retry) and
     * against a captured/replayed transaction payload.
     *
     * Instance-level for the same reason as dailyDebitedTotal
     * above - one bean, shared across every transaction the
     * running application processes.
     */
    private final Set<String> processedMerchantReferences =
            new HashSet<>();

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
            DualVerificationService dualVerificationService,
            EpiResponseBuilder epiResponseBuilder) {

        this.encryptionService = encryptionService;
        this.checksumService = checksumService;
        this.transactionParser = transactionParser;
        this.dualVerificationService = dualVerificationService;
        this.epiResponseBuilder = epiResponseBuilder;
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
         * =====================================================
         * BANK CONSOLE LOG - INCOMING URL (EDUCATIONAL / DEBUG)
         * =====================================================
         *
         * Reconstructs the same PID + encdata URL the Gateway
         * logged when it handed this transaction off, so both
         * sides of the exchange are visible in the console.
         */
        System.out.println(
                "========================================"
        );
        System.out.println(
                "[BANK] Received transaction as:"
        );
        System.out.println(
                "https://www.returnurl.com?PID="
                        + pid
                        + "&encdata="
                        + encdata
        );

        /*
         * Decrypt.
         */
        String decryptedData =
                encryptionService.decrypt(
                        encdata,
                        ENCRYPTION_KEY
                );

        System.out.println("[BANK] Decrypted data:");
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

        System.out.println(
                "[BANK] Raw transaction (EPI string):"
        );
        System.out.println(
                rawTransaction
        );
        System.out.println(
                "[BANK] Checksum received: " + receivedChecksum
        );
        System.out.println(
                "[BANK] Checksum calculated: " + calculatedChecksum
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

        System.out.println(
                "========================================"
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
     * INVALID_MERCHANT
     * MERCHANT_NOT_FOUND
     * ACCOUNT_BLOCKED
     * ACCOUNT_CLOSED
     * DAILY_LIMIT_EXCEEDED
     * TXN_LIMIT_EXCEEDED
     * SUSPECTED_FRAUD
     * DUPLICATE_TRANSACTION
     * BANK_SYSTEM_UNAVAILABLE
     * TRANSACTION_TIMEOUT
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

            /*
             * =====================================================
             * ACCOUNT / MERCHANT IDENTITY ERRORS
             * =====================================================
             *
             * "Invalid account" above means the customer's account
             * number doesn't match the demo record. These two are
             * different: the MERCHANT side of the transaction is
             * the problem, not the customer's account.
             */

            case "INVALID_MERCHANT":

                return createFailureResponse(
                        "Invalid merchant details"
                );

            case "MERCHANT_NOT_FOUND":

                return createFailureResponse(
                        "Merchant no longer exists in system"
                );

            /*
             * =====================================================
             * ACCOUNT STATUS ERRORS
             * =====================================================
             *
             * A real bank account can exist, have funds, and still
             * be unusable because of its current status - not a
             * data-validity problem, a state problem.
             */

            case "ACCOUNT_BLOCKED":

                return createFailureResponse(
                        "Account blocked due to suspicious activity"
                );

            case "ACCOUNT_CLOSED":

                return createFailureResponse(
                        "Account closed"
                );

            /*
             * =====================================================
             * LIMIT ERRORS
             * =====================================================
             *
             * Banks enforce limits independently of the customer's
             * actual balance - a well-funded account can still be
             * declined for exceeding a limit.
             */

            case "DAILY_LIMIT_EXCEEDED":

                return createFailureResponse(
                        "Daily transaction limit exceeded"
                );

            case "TXN_LIMIT_EXCEEDED":

                return createFailureResponse(
                        "Transaction amount exceeds per-transaction limit"
                );

            /*
             * =====================================================
             * RISK / FRAUD ERROR
             * =====================================================
             */

            case "SUSPECTED_FRAUD":

                return createFailureResponse(
                        "Transaction declined by risk engine - suspected fraud"
                );

            /*
             * =====================================================
             * DUPLICATE TRANSACTION
             * =====================================================
             *
             * Simulates the bank recognising that this merchant
             * reference has already been processed successfully,
             * and refusing to debit the account a second time.
             */

            case "DUPLICATE_TRANSACTION":

                return createFailureResponse(
                        "Duplicate transaction - merchant reference already processed"
                );

            /*
             * =====================================================
             * BANK-SIDE OPERATIONAL ERRORS
             * =====================================================
             *
             * These aren't about THIS transaction being wrong at
             * all - the bank's own systems are the problem. Real
             * gateways must handle these differently (usually:
             * retry later, don't treat as a hard decline).
             */

            case "BANK_SYSTEM_UNAVAILABLE":

                return createFailureResponse(
                        "Bank system temporarily unavailable"
                );

            case "TRANSACTION_TIMEOUT":

                return createFailureResponse(
                        "Transaction timed out at bank"
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
         * =========================================================
         * INCOMING TRANSACTION VALIDATION (ERROR PLAN - CATEGORY A)
         * =========================================================
         *
         * These are the bank's own processing validations
         * (account, PID, checksum, currency, amount rules,
         * merchant fields, balance, etc).
         *
         * IMPORTANT:
         * A validation failure here must NOT throw all the way up
         * to the controller (which would produce a raw HTTP 500).
         * It must be converted into a normal FAILED PaymentResponse
         * so the UI can show:
         *
         *   Payment Failed
         *   Status: FAILED
         *   Reason: <validation message>
         *
         * No debit happens in this path.
         */

        try {

            validateBankRules(request);

        } catch (IllegalArgumentException e) {

            System.out.println(
                    "Incoming transaction validation failed: "
                            + e.getMessage()
            );

            return createFailureResponse(
                    e.getMessage()
            );
        }

        BigDecimal totalDebit =
                calculateTotalDebit(request);
        /*
         * Debit account.
         */
        accountBalance =
                accountBalance.subtract(totalDebit);

        /*
         * Track this debit against the daily limit, and mark
         * this merchant reference as processed so a retry/replay
         * of the same reference is caught by the duplicate check
         * at the top of validateBankRules().
         */
        dailyDebitedTotal =
                dailyDebitedTotal.add(totalDebit);

        if (request.getFldMerchRefNbr() != null) {
            processedMerchantReferences.add(
                    request.getFldMerchRefNbr()
            );
        }

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

            attachEncryptedResponse(response);

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

        attachEncryptedResponse(response);

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

        attachEncryptedResponse(response);

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
         * =====================================================
         * DUPLICATE TRANSACTION / IDEMPOTENCY CHECK
         * =====================================================
         *
         * Checked first, before anything else - a real bank's
         * idempotency guard doesn't care whether the rest of the
         * transaction is even valid, it just refuses to touch a
         * merchant reference it has already completed.
         */
        if (request.getFldMerchRefNbr() != null &&
                processedMerchantReferences.contains(
                        request.getFldMerchRefNbr()
                )) {

            throw new IllegalArgumentException(
                    "Duplicate transaction - merchant reference "
                            + "already processed"
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
         * Per-transaction limit
         *
         * Applies to the transaction's principal amount only
         * (not the service amount) - matches how real per-txn
         * limits are usually expressed.
         */
        if (request.getFldTxnAmt()
                .compareTo(PER_TRANSACTION_LIMIT) > 0) {

            throw new IllegalArgumentException(
                    "Transaction amount exceeds per-transaction limit"
            );
        }

        /*
         * Service amount cannot be negative
         */
        if (request.getFldTxnScAmt() != null &&
                request.getFldTxnScAmt()
                        .compareTo(BigDecimal.ZERO) < 0) {

            throw new IllegalArgumentException(
                    "Service amount cannot be negative"
            );
        }

        /*
         * Service amount cannot exceed transaction amount
         */
        if (request.getFldTxnScAmt() != null &&
                request.getFldTxnScAmt()
                        .compareTo(request.getFldTxnAmt()) > 0) {

            throw new IllegalArgumentException(
                    "Service amount cannot be more than transaction amount"
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

        /*
         * Daily limit
         *
         * Checked AFTER balance deliberately: if an account
         * can't even cover this one transaction, "insufficient
         * funds" is the more useful message to a tester than
         * "daily limit exceeded".
         */
        if (dailyDebitedTotal.add(totalDebit)
                .compareTo(DAILY_TRANSACTION_LIMIT) > 0) {

            throw new IllegalArgumentException(
                    "Daily transaction limit exceeded"
            );
        }
    }

    /*
     * =========================================================
     * ENCRYPT OUTGOING RESPONSE
     * =========================================================
     *
     * Builds the same encrypted payload a real bank would POST
     * to the gateway's S2S callback endpoint (/payment/result):
     * raw EPI response string -> append checksum -> AES encrypt.
     *
     * Attached to the response so the browser can carry it over
     * to that real endpoint and you can inspect the actual
     * payload in DevTools instead of just query parameters.
     */
    private void attachEncryptedResponse(
            PaymentResponse response) {

        String rawResponse =
                epiResponseBuilder.buildRawResponse(
                        response
                );

        String checksum =
                checksumService.generateChecksum(
                        rawResponse
                );

        String rawWithChecksum =
                rawResponse
                        + "&CHECKSUM="
                        + checksum;

        String encdata =
                encryptionService.encrypt(
                        rawWithChecksum,
                        ENCRYPTION_KEY
                );

        response.setEncdata(encdata);

        System.out.println(
                "[BANK] Encrypted response (encdata) for "
                        + "S2S callback:"
        );
        System.out.println(
                encdata
        );
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