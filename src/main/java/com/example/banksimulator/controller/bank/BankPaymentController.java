package com.example.banksimulator.controller.bank;

import com.example.banksimulator.model.PaymentRequest;
import com.example.banksimulator.model.PaymentResponse;
import com.example.banksimulator.service.BankPaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/bank")
public class BankPaymentController {

    private final BankPaymentService bankPaymentService;

    public BankPaymentController(
            BankPaymentService bankPaymentService) {

        this.bankPaymentService = bankPaymentService;
    }

    /*
     * =========================================================
     * GET PENDING TRANSACTION
     * =========================================================
     *
     * Bank Simulator UI calls:
     *
     * GET /bank/pending
     *
     * Returns:
     *
     * {
     *   "pid": "PID_XYZ_001",
     *   "request": { ... },
     *   "checksumValid": true
     * }
     */
    @GetMapping("/pending")
    public ResponseEntity<?> getPendingTransaction() {

        try {

            PaymentRequest request =
                    bankPaymentService.getPendingTransaction();

            Map<String, Object> response =
                    new LinkedHashMap<>();

            response.put(
                    "pid",
                    bankPaymentService.getPendingPid()
            );

            response.put(
                    "request",
                    request
            );

            response.put(
                    "checksumValid",
                    bankPaymentService.isPendingChecksumValid()
            );

            response.put(
                    "accountBalance",
                    bankPaymentService.getAccountBalance()
            );

            return ResponseEntity.ok(response);

        } catch (IllegalStateException e) {

            /*
             * No transaction has arrived yet.
             *
             * Return JSON null so the JavaScript can
             * display "Waiting for transaction".
             */
            return ResponseEntity.ok(null);
        }
    }


    /*
     * =========================================================
     * PROCESS BANK SIMULATION
     * =========================================================
     *
     * Bank Simulator UI calls:
     *
     * POST /bank/payment/process?scenario=SUCCESS
     *
     * Examples:
     *
     * SUCCESS
     * INVALID_ACCOUNT
     * INSUFFICIENT_FUNDS
     * INVALID_PID
     * INVALID_CHECKSUM
     * INVALID_CURRENCY
     */
    @PostMapping("/payment/process")
    public ResponseEntity<?> processSimulation(
            @RequestParam String scenario) {

        try {

            PaymentResponse response =
                    bankPaymentService.processSimulation(
                            scenario
                    );

            return ResponseEntity.ok(response);

        } catch (IllegalStateException e) {

            /*
             * =====================================================
             * NO PENDING TRANSACTION
             * =====================================================
             *
             * Nothing has arrived from the Gateway yet, or the
             * transaction was already processed. Return a normal
             * JSON error instead of a raw HTTP 500 so the Bank
             * Simulator UI can show a friendly message.
             */
            Map<String, Object> error =
                    new LinkedHashMap<>();

            error.put("message", e.getMessage());

            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(error);

        } catch (IllegalArgumentException e) {

            /*
             * =====================================================
             * SAFETY NET
             * =====================================================
             *
             * Any bank-side validation error that wasn't already
             * converted into a FAILED PaymentResponse. Keeps the
             * API contract consistent (never a raw 500 for a
             * business-rule failure).
             */
            Map<String, Object> error =
                    new LinkedHashMap<>();

            error.put("message", e.getMessage());

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(error);
        }
    }
}