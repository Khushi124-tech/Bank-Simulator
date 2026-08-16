package com.example.banksimulator.controller.gateway;

import com.example.banksimulator.model.BankPaymentRequest;
import com.example.banksimulator.model.PaymentRequest;
import com.example.banksimulator.service.BankPaymentService;
import com.example.banksimulator.service.PaymentService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/gateway")
public class GatewayController {

    private static final String BANK_ID = "YBM";

    private static final String BANK_PID =
            "PID_XYZ_001";

    private static final String BANK_ENCRYPTION_KEY =
            "BILL#1234";

    /*
     * Fixed simulator return URL.
     *
     * This is NOT entered by the customer.
     */
    private static final String RETURN_URL =
            "http://localhost:9090/";


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
    public Object processPayment(
            @RequestBody PaymentRequest request) {

        /*
         * =====================================================
         * FIXED GATEWAY CONFIGURATION
         * =====================================================
         *
         * The customer does not provide RU.
         *
         * The Gateway supplies it.
         */
        request.setBankid(BANK_ID);
        request.setRu(RETURN_URL);


        /*
         * =====================================================
         * 1. CREATE ENCRYPTED TRANSACTION
         * =====================================================
         */

        String encdata =
                paymentService.preparePayment(
                        request,
                        BANK_ENCRYPTION_KEY
                );


        /*
         * =====================================================
         * 2. CREATE BANK REQUEST
         * =====================================================
         */

        BankPaymentRequest bankRequest =
                new BankPaymentRequest();

        bankRequest.setPid(BANK_PID);

        bankRequest.setEncdata(encdata);

        /*
         * =====================================================
         * GATEWAY -> BANK URL (EDUCATIONAL / DEBUG TRACE)
         * =====================================================
         *
         * In a real BillDesk-style integration, the browser is
         * redirected to the bank's payment page carrying PID and
         * encdata as query parameters, e.g.:
         *
         *   https://<bank-host>/pay?PID=<pid>&encdata=<encrypted>
         *
         * This simulator hands the same two values directly to
         * BankPaymentService as an in-process call instead of a
         * real HTTP redirect, but we log the equivalent URL so
         * the shape of a real integration is visible.
         */
        String bankRequestUrl =
                "https://www.returnurl.com?PID="
                        + BANK_PID
                        + "&encdata="
                        + encdata;

        System.out.println(
                "[GATEWAY] Handing off to Bank as:"
        );
        System.out.println(
                bankRequestUrl
        );

        /*
         * =====================================================
         * 3. SEND TO BANK RECEIVE STAGE
         * =====================================================
         *
         * This does NOT debit the account.
         *
         * It creates a pending transaction.
         */

        return bankPaymentService.receiveTransaction(
                bankRequest.getPid(),
                bankRequest.getEncdata()
        );
    }
}

/*
What it does?
Flowcharts
                 CUSTOMER / CLIENT
                        │
                        │ POST /gateway/payment
                        │
                        │ PaymentRequest
                        ▼
              ┌─────────────────────┐
              │   GatewayController  │
              │                     │
              │  processPayment()   │
              └──────────┬──────────┘
                         │
                         │ 1. Add fixed information
                         │    bankid = YBM
                         │    ru = localhost:9090
                         ▼
              ┌─────────────────────┐
              │   PaymentRequest    │
              │                     │
              │ customer data       │
              │ bankid               │
              │ ru                   │
              └──────────┬──────────┘
                         │
                         │ 2. Send request
                         │    to PaymentService
                         ▼
              ┌─────────────────────┐
              │   PaymentService    │
              │                     │
              │ preparePayment()    │
              └──────────┬──────────┘
                         │
                         │ encrypted transaction
                         ▼
                    "encdata"
                         │
                         │
                         ▼
              ┌─────────────────────┐
              │ BankPaymentRequest  │
              │                     │
              │ PID = PID_XYZ_001   │
              │ encdata = encrypted │
              └──────────┬──────────┘
                         │
                         │ 3. Send PID + encdata
                         ▼
              ┌─────────────────────┐
              │ BankPaymentService  │
              │                     │
              │ receiveTransaction()│
              └──────────┬──────────┘
                         │
                         │ creates pending transaction
                         ▼
                  PaymentResponse
 */