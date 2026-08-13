package com.example.banksimulator.controller.gateway;

import com.example.banksimulator.model.BankPaymentRequest;
import com.example.banksimulator.model.GatewayPaymentResponse;
import com.example.banksimulator.model.PaymentRequest;
import com.example.banksimulator.service.BankPaymentService;
import com.example.banksimulator.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/gateway")
public class GatewayController {

    private static final String BANK_PID =
            "PID_XYZ_001";

    private static final String BANK_ENCRYPTION_KEY =
            "BILL#1234";

    private static final String BANK_URL =
            "/bank/simulator";

    private final PaymentService paymentService;

    private final BankPaymentService bankPaymentService;


    public GatewayController(
            PaymentService paymentService,
            BankPaymentService bankPaymentService) {

        this.paymentService =
                paymentService;

        this.bankPaymentService =
                bankPaymentService;
    }


    @PostMapping("/payment")
    public ResponseEntity<?> processPayment(
            @RequestBody PaymentRequest request) {

        try {

            /*
             * 1. Gateway creates EPI transaction,
             *    checksum and encrypted encdata.
             */
            String encdata =
                    paymentService.preparePayment(
                            request,
                            BANK_ENCRYPTION_KEY
                    );


            /*
             * 2. Create Bank request.
             */
            BankPaymentRequest bankRequest =
                    new BankPaymentRequest();

            bankRequest.setPid(BANK_PID);
            bankRequest.setEncdata(encdata);


            /*
             * 3. Bank receives the transaction.
             *
             * IMPORTANT:
             * This only creates a PENDING transaction.
             * No debit happens here.
             */
            PaymentRequest pendingTransaction =
                    bankPaymentService.receiveTransaction(
                            bankRequest.getPid(),
                            bankRequest.getEncdata()
                    );


            /*
             * 4. Return Bank Simulator URL.
             */
            GatewayPaymentResponse response =
                    new GatewayPaymentResponse(
                            BANK_URL,
                            BANK_PID,
                            "PENDING",
                            pendingTransaction
                    );


            return ResponseEntity.ok(response);


        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            new GatewayErrorResponse(
                                    "FAILED",
                                    e.getMessage()
                            )
                    );

        } catch (Exception e) {

            return ResponseEntity
                    .internalServerError()
                    .body(
                            new GatewayErrorResponse(
                                    "FAILED",
                                    "Unable to initiate payment"
                            )
                    );
        }
    }


    /*
     * =========================================================
     * GATEWAY ERROR RESPONSE
     * =========================================================
     */

    private static class GatewayErrorResponse {

        private String status;
        private String message;


        public GatewayErrorResponse(
                String status,
                String message) {

            this.status = status;
            this.message = message;
        }


        public String getStatus() {
            return status;
        }


        public String getMessage() {
            return message;
        }
    }
}