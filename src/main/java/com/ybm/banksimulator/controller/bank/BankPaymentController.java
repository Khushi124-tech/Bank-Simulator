package com.ybm.banksimulator.controller.bank;

import com.ybm.banksimulator.model.PaymentRequest;
import com.ybm.banksimulator.model.PaymentResponse;
import com.ybm.banksimulator.service.BankPaymentService;
import org.springframework.http.HttpHeaders;
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
     * GET /bank/receive
     * =========================================================
     *
     * This is the Bank's REAL, standalone endpoint - the one the
     * Gateway's HTTP 302 redirect actually sends the browser to
     * (see GatewayController). It is reached by a genuine browser
     * navigation, not a Java method call and not a fetch() - PID
     * and encdata arrive as ordinary URL query parameters,
     * visible directly in the browser's address bar AND in
     * DevTools -> Network -> "receive" -> Headers, exactly the
     * way a real bank redirect URL looks (e.g.
     * https://test.yesbank.in/.../create?PID=...&encdata=...).
     *
     * How the Bank "knows the format": it doesn't have to guess.
     * The URL query string is a fixed part of the HTTP spec -
     * whatever comes after "?" as key=value pairs. Because this
     * method uses @RequestParam, Spring reads PID and encdata
     * straight out of that query string, automatically URL-
     * decoding them back to their original values. In a real
     * integration this "what format, what field names, what URL"
     * is exactly what the Bank publishes in its own integration
     * documentation to every gateway that connects to it - a
     * fixed contract agreed upfront, not something negotiated per
     * request.
     */
    @GetMapping("/receive")
    public ResponseEntity<Void> receiveFromGateway(
            @RequestParam("PID") String pid,
            @RequestParam("encdata") String encdata) {

        try {

            bankPaymentService.receiveTransaction(
                    pid,
                    encdata
            );

        } catch (RuntimeException e) {

            System.out.println(
                    "[BANK] Failed to receive transaction via "
                            + "/bank/receive: " + e.getMessage()
            );

            /*
             * Still send the browser on to the Bank Simulator UI -
             * it will simply show "no pending transaction" and the
             * console has already explained why.
             */
        }

        /*
         * A real HTTP 302 - the browser follows this itself.
         * Visible in DevTools -> Network as a redirected request,
         * landing on the same Bank Simulator UI your existing
         * bank-payment.js already knows how to load from.
         */
        return ResponseEntity
                .status(HttpStatus.FOUND)
                .header(
                        HttpHeaders.LOCATION,
                        "/bank/simulator"
                )
                .build();
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