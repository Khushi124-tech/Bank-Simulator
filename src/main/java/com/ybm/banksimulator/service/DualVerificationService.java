package com.ybm.banksimulator.service;

import com.ybm.banksimulator.model.DualVerificationResult;
import com.ybm.banksimulator.model.PaymentResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class DualVerificationService {

    /*
     * =========================================================
     * COMPLETED TRANSACTION
     * =========================================================
     *
     * The simulator keeps a snapshot of the completed bank
     * transaction for dual verification.
     *
     * Since this is a simplified single-transaction simulator,
     * one stored transaction is sufficient.
     */
    private PaymentResponse completedTransaction;


    /*
     * =========================================================
     * REGISTER COMPLETED TRANSACTION
     * =========================================================
     */

    public synchronized void registerCompletedTransaction(
            PaymentResponse response) {

        if (response == null) {

            throw new IllegalArgumentException(
                    "Completed transaction cannot be null"
            );
        }

        /*
         * Store a snapshot instead of the same object reference.
         */
        this.completedTransaction =
                copyResponse(response);
    }


    /*
     * =========================================================
     * DUAL VERIFICATION
     * =========================================================
     *
     * Simplified internal flow:
     *
     * BankPaymentService
     *        ↓
     * PaymentResponse
     *        ↓
     * DualVerificationService
     *        ↓
     * Compare against stored transaction
     */

    public synchronized DualVerificationResult verify(
            PaymentResponse receivedResponse) {

        if (receivedResponse == null) {

            return failure(
                    "Dual verification failed: response is null"
            );
        }

        if (completedTransaction == null) {

            return failure(
                    "Dual verification failed: no completed transaction"
            );
        }


        /*
         * Transaction must have succeeded.
         */

        if (!"S".equalsIgnoreCase(
                receivedResponse.getFlgSuccess())) {

            return failure(
                    "Dual verification failed: transaction is not successful"
            );
        }


        /*
         * =====================================================
         * CLIENT CODE
         * =====================================================
         */

        if (!same(
                completedTransaction.getFldClientCode(),
                receivedResponse.getFldClientCode())) {

            return failure(
                    "Dual verification failed: client code mismatch"
            );
        }


        /*
         * =====================================================
         * MERCHANT CODE
         * =====================================================
         */

        if (!same(
                completedTransaction.getFldMerchCode(),
                receivedResponse.getFldMerchCode())) {

            return failure(
                    "Dual verification failed: merchant code mismatch"
            );
        }


        /*
         * =====================================================
         * CURRENCY
         * =====================================================
         */

        if (!same(
                completedTransaction.getFldTxnCurr(),
                receivedResponse.getFldTxnCurr())) {

            return failure(
                    "Dual verification failed: currency mismatch"
            );
        }


        /*
         * =====================================================
         * TRANSACTION AMOUNT
         * =====================================================
         */

        if (!sameAmount(
                completedTransaction.getFldTxnAmt(),
                receivedResponse.getFldTxnAmt())) {

            return failure(
                    "Dual verification failed: amount mismatch"
            );
        }


        /*
         * =====================================================
         * SERVICE AMOUNT
         * =====================================================
         */

        if (!sameAmount(
                completedTransaction.getFldTxnScAmt(),
                receivedResponse.getFldTxnScAmt())) {

            return failure(
                    "Dual verification failed: service amount mismatch"
            );
        }


        /*
         * =====================================================
         * MERCHANT REFERENCE
         * =====================================================
         */

        if (!same(
                completedTransaction.getFldMerchRefNbr(),
                receivedResponse.getFldMerchRefNbr())) {

            return failure(
                    "Dual verification failed: merchant reference mismatch"
            );
        }


        /*
         * =====================================================
         * BANK REFERENCE
         * =====================================================
         */

        if (!same(
                completedTransaction.getBankRefNo(),
                receivedResponse.getBankRefNo())) {

            return failure(
                    "Dual verification failed: bank reference mismatch"
            );
        }


        /*
         * =====================================================
         * VERIFICATION SUCCESS
         * =====================================================
         */

        receivedResponse.setFldVerify("V");

        receivedResponse.setMessage(
                "Merchant transaction successful - Dual Verification successful"
        );

        return new DualVerificationResult(
                true,
                "Dual verification successful"
        );
    }


    /*
     * =========================================================
     * STRING COMPARISON
     * =========================================================
     */

    private boolean same(
            Object expected,
            Object received) {

        if (expected == null && received == null) {
            return true;
        }

        if (expected == null || received == null) {
            return false;
        }

        return expected.toString()
                .equals(received.toString());
    }


    /*
     * =========================================================
     * AMOUNT COMPARISON
     * =========================================================
     *
     * BigDecimal.equals() considers scale.
     *
     * 100.00 != 100.0 using equals()
     *
     * compareTo() correctly treats both as the same amount.
     */

    private boolean sameAmount(
            BigDecimal expected,
            BigDecimal received) {

        if (expected == null || received == null) {
            return expected == received;
        }

        return expected.compareTo(received) == 0;
    }


    /*
     * =========================================================
     * FAILURE
     * =========================================================
     */

    private DualVerificationResult failure(
            String message) {

        return new DualVerificationResult(
                false,
                message
        );
    }


    /*
     * =========================================================
     * COPY RESPONSE
     * =========================================================
     *
     * Prevents the verification record from being the exact
     * same mutable object as the response being verified.
     */

    private PaymentResponse copyResponse(
            PaymentResponse source) {

        PaymentResponse copy =
                new PaymentResponse();

        copy.setFldClientCode(
                source.getFldClientCode()
        );

        copy.setFldMerchCode(
                source.getFldMerchCode()
        );

        copy.setFldTxnCurr(
                source.getFldTxnCurr()
        );

        copy.setFldTxnAmt(
                source.getFldTxnAmt()
        );

        copy.setFldTxnScAmt(
                source.getFldTxnScAmt()
        );

        copy.setFldMerchRefNbr(
                source.getFldMerchRefNbr()
        );

        copy.setFlgSuccess(
                source.getFlgSuccess()
        );

        copy.setFldVerify(
                source.getFldVerify()
        );

        copy.setFldDatTimeTxn(
                source.getFldDatTimeTxn()
        );

        copy.setBankRefNo(
                source.getBankRefNo()
        );

        copy.setFldRef2(
                source.getFldRef2()
        );

        copy.setMessage(
                source.getMessage()
        );

        return copy;
    }
}