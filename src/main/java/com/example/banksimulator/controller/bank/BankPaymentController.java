package com.example.banksimulator.controller.bank;

import com.example.banksimulator.model.PaymentRequest;
import com.example.banksimulator.model.PaymentResponse;
import com.example.banksimulator.service.BankPaymentService;
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
    public ResponseEntity<PaymentResponse> processSimulation(
            @RequestParam String scenario) {

        PaymentResponse response =
                bankPaymentService.processSimulation(
                        scenario
                );

        return ResponseEntity.ok(response);
    }
}